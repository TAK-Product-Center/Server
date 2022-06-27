#!/usr/bin/env python3

import argparse
import datetime
import os
import re
from sys import stderr
from typing import Dict, Tuple, List, Union
from xml.etree import ElementTree
from zipfile import ZipFile

from pygal import StackedBar, Line

from gen.logging import *

_parser = argparse.ArgumentParser()

SCRIPT_ROOT = os.path.dirname(os.path.realpath(__file__))
BUILD_ROOT = os.path.abspath(os.path.join(SCRIPT_ROOT, '../../takserver-core/build'))

_parser.add_argument('--test-artifacts', metavar='TEST_ARTIFACTS', type=str, required=True,
                     help='The directory (often TEST_ARTIFACTS)  or zip that contains the test results')
_parser.add_argument('--output-directory', '-o', type=str, help='The output directory for the results')

_parser.add_argument('--specific-test', '-s', type=str, help='the full name f the test to analyze')
_parser.add_argument('--round-trip-only', '-r', action='store_true',
                     help='Ignores the Server logs for more flexibility at the cost of details')

RE_TIMESTAMP = r'(?P<year>[0-9]{4})-(?P<month>[0-9]{2})-(?P<day>[0-9]{2})-(?P<hour>[0-9]{2})\:' + \
               r'(?P<minute>[0-9]{2})\:(?P<second>[0-9]{2})\.(?P<microsecond>[0-9]{3})'

SEP = ';;;'

RE_TEST_SEPARATOR = r'.*\$#\$# Starting Execution: {(?P<label>[A-Za-z0-9.-]*)}.*'


def get_time_ms(time_string: str):
    d = re.match(RE_TIMESTAMP, time_string).groupdict()
    d['microsecond'] = d['microsecond'] + '000'
    str_dict = dict()
    for key in d.keys():
        str_dict[key] = int(d[key])
    return int(datetime.datetime(**str_dict).timestamp() * 1000)


class Formatting(Enum):
    LOG = 'log'
    TIMINGS = 'timings'


class TestData:
    def __init__(self, element: ElementTree.Element, stdout: List[str], stderr: List[str]):
        self._classname = element.get('classname')
        self._methodname = element.get('name')
        self._duration = element.get('time')

        children = list(element)
        failure = None
        if len(children) > 0:
            for child in children:
                tag = child.tag
                print("TAG: " + tag)

                if tag == 'failure':
                    failure = child.text
                    pass
                else:
                    raise Exception('Unexpected test case child tag "' + tag + '"!')

        self.failure = failure

        self.category_message_instances = dict()

        result, self.client_stdout_remainder = partition_lines(stdout, Source.CLIENT)
        for category, message_instances in result.items():
            if category not in self.category_message_instances.keys():
                self.category_message_instances[category] = list()
            self.category_message_instances[category].extend(message_instances)

        result, self.client_stderr_remainder = partition_lines(stderr, Source.CLIENT)
        for category, message_instances in result.items():
            if category not in self.category_message_instances.keys():
                self.category_message_instances[category] = list()
            self.category_message_instances[category].extend(message_instances)

        self.server_remainder = list()

    def load_server_logs(self, server_log_directory: str):
        for file in next(os.walk(server_log_directory))[2]:
            print("FILE: " + file)
            server_log_lines = open(os.path.join(server_log_directory, file), 'r').readlines()
            result, remainder = partition_lines(server_log_lines, Source.SERVER)
            self.server_remainder.extend(remainder)
            for category, message_instances in result.items():
                if category not in self.category_message_instances.keys():
                    self.category_message_instances[category] = list()
                self.category_message_instances[category].extend(message_instances)

    @property
    def classname(self):
        return self._classname

    @property
    def methodname(self):
        return self._methodname

    @property
    def label(self):
        return self._classname + '.' + self._methodname


class TestDataCollector:
    def __init__(self, target_directory: str, round_trip_only: bool = False):
        self.target_directory = target_directory
        self.round_trip_only = round_trip_only
        self.results = dict()  # type: Dict[str, TestData]

    def _load_xml_file(self, xml_filepath: str):
        et = ElementTree.fromstring('\n'.join(open(xml_filepath, 'r')))
        test_cases = dict()
        stderr = list()
        stdout = list()
        root_children = list(et)
        for child in root_children:
            tag = child.tag
            if tag == 'system-out':
                for out_line in child.text.split('\n'):
                    stripped_line = out_line.strip()
                    if stripped_line != '':
                        stdout.append(stripped_line)
            elif tag == 'system-err':
                for err_line in child.text.split('\n'):
                    stripped_line = err_line.strip()
                    if stripped_line != '':
                        stderr.append(stripped_line)
            elif tag == 'testcase':
                # return child
                test_cases[child.get('classname') + '.' + child.get('name')] = child

        def split_log(lines: List[str]) -> Dict[str, List[str]]:
            results: Dict[str, List[str]] = dict()
            current_list = None

            for line in lines:
                match = re.match(RE_TEST_SEPARATOR, line)
                if match is None:
                    if current_list is not None:
                        current_list.append(line)

                else:
                    current_test_case = match.groups()[0]
                    if current_test_case in results.keys():
                        raise Exception('The logs for test case ' + current_test_case + ' has already been detected!')
                    current_list = list()
                    results[current_test_case] = current_list

            return results

        stdout_dict = split_log(stdout)
        stderr_dict = split_log(stderr)
        assert set(test_cases.keys()) == set(stdout_dict.keys())
        assert set(stderr_dict.keys()).issubset(test_cases.keys())

        # return test_cases
        for key in test_cases.keys():
            if key not in stderr_dict.keys():
                stderr_dict[key] = list()

        for key, test_case in test_cases.items():
            if key in self.results:
                raise Exception('Test data for "' + key + '" already exists!')
            self.results[key] = TestData(element=test_case, stdout=stdout_dict[key], stderr=stderr_dict[key])

        return self.results

    def load_xml_results(self, artifact_directory: str, desired_test_identifier: str = None):
        for path, subdirectories, files in os.walk(artifact_directory):
            if path == artifact_directory:
                for file in files:
                    if file.startswith('TEST-') and file.endswith('.xml'):
                        test_name = file[5:-4]
                        if desired_test_identifier is None or test_name == desired_test_identifier:
                            self._load_xml_file(os.path.join(path, file))

        return self.results

    def load_server_logs(self, artifact_root: str, desired_test_identifier: str = None):
        test_cases = set(next(os.walk(artifact_root))[1])

        if set(self.results.keys()).intersection(test_cases) == 0:
            raise Exception('No test results could be found in the server artifact directory! ' +
                            'Please be sure the files are in the structure <test_case>/<takserver_log_dir>!')

        for test_identifier, test_data in self.results.items():
            if desired_test_identifier is None or test_identifier == desired_test_identifier:
                if test_identifier in test_cases:
                    log_dir = os.path.join(artifact_root, test_identifier, 'logs')
                    if not os.path.exists(log_dir):
                        print('No log directory present in ' + os.path.join(artifact_root, test_identifier) + '!')
                    else:
                        test_data.load_server_logs(log_dir)
                else:
                    if test_data.failure is None:
                        print('WARNING: Could not find server logs for the test ' + test_identifier + '!')

    def produce_final_result(self):
        print('Collating log contents in ' + self.target_directory + '...')
        for test_name, test_data in self.results.items():
            target_directory = os.path.join(self.target_directory, test_name)
            if not os.path.exists(target_directory):
                os.makedirs(target_directory)
            category = 'NETWORK'

            if test_data.client_stdout_remainder is not None and len(test_data.client_stdout_remainder) > 0:
                open(os.path.join(target_directory, 'client-stdout-remainder.log'), 'w') \
                    .writelines(test_data.client_stdout_remainder)

            if test_data.client_stderr_remainder is not None and len(test_data.client_stderr_remainder) > 0:
                open(os.path.join(target_directory, 'client-stderr-remainder.log'), 'w') \
                    .writelines(test_data.client_stderr_remainder)

            if test_data.server_remainder is not None and len(test_data.server_remainder) > 0:
                open(os.path.join(target_directory, 'server-remainder.log'), 'w') \
                    .writelines(test_data.server_remainder)

            message_instances = test_data.category_message_instances[category]
            message_instances.sort(key=lambda x: x.sort_string)
            if self.round_trip_only:
                merge_category_round_trip(category, message_instances, target_directory, formatting=Formatting.LOG)
                merge_category_round_trip(category, message_instances, target_directory, formatting=Formatting.TIMINGS)
            else:
                merge_category(category, message_instances, target_directory, formatting=Formatting.LOG)
                merge_category(category, message_instances, target_directory, formatting=Formatting.TIMINGS)


class MessageEntry:
    def __init__(self, category: Category, source: Source, direction: Direction, label: str, match=None, regex=None,
                 ignore=False):
        self._category = category
        self._source = source
        self._direction = direction
        self._label = label
        if match is None and regex is None:
            raise Exception("A regex or match must be provided!")
        self._match = match
        self._regex = regex
        self.ignore = ignore

    def __str__(self) -> str:
        return self.category.value + ': ' + self.source.value + '-' + self.direction.value + ' - ' + self.label

    @property
    def category(self) -> Category:
        return self._category

    @property
    def source(self) -> Source:
        return self._source

    @property
    def direction(self) -> Direction:
        return self._direction

    @property
    def label(self) -> str:
        return self._label

    @property
    def match(self) -> Union[str, None]:
        return self._match

    @property
    def regex(self) -> Union[str, None]:
        return self._regex


class MessageEntries:
    _entries = [*list(ClientEvent),
                MessageEntry(Category.NETWORK, Source.SERVER, Direction.SEND, 'send(stcp)',
                             match=' - server_streaming_CoT writing application data'),
                #        MessageEntry(Category.NETWORK, Source.SERVER, Direction.SEND, 'send(wss)',
                #                     match=' - sending TakMessage SituationAwarenessMessage'),
                MessageEntry(Category.NETWORK, Source.SERVER, Direction.SEND, 'send(tcp/udp/mcast)',
                             match=' - sending a latest SA message'),
                # MessageEntry(Category.NETWORK, Source.SERVER, Direction.RECEIVE, 'receive',
                #              match='- TakMessageProcessor received message'),
                # This is not ideal for a scenario when auth and a msg are sent at the same time!
                # MessageEntry(Category.NETWORK, Source.SERVER, Direction.RECEIVE, 'receive',
                #              match='MessagingUtilImpl - sa to send'),
                MessageEntry(Category.NETWORK, Source.SERVER, Direction.RECEIVE, 'receive(udp',
                             match=' - server_packet_CoT received network data -- handler: [UDP server'),
                MessageEntry(Category.NETWORK, Source.SERVER, Direction.RECEIVE, 'receive(tcp)',
                             match=' - onDataReceived channel handler com.bbn.marti.nio.channel.connections.TcpChannelHandler'),
                MessageEntry(Category.NETWORK, Source.SERVER, Direction.SEND, 'send[v2]',
                             match=' - BrokerService subscription Subscription to send to: Subscription --'),
                MessageEntry(Category.NETWORK, Source.SERVER, Direction.RECEIVE, 'receive[v2]',
                             match=' - onDataReceived channel handler com.bbn.marti.nio.netty.handlers.NioNettyHandlerBase')

                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.IG, 'ignite',
                #              regex=r'^[0-9]{2}\:[0-9]{2}\:[0-9]{2}\.[0-9]{3}\ ',
                #              ignore=True),
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.RECEIVE, 'receive(wss)',
                #              match='- ByteBuffer to TakMessage'),
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.SEND, 'send(wss)',
                #              match='- Send ByteBuffer'),
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.SEND, 'send(stcp+auth)',
                #              match=' - Send Auth Payload and Message'),
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.SEND, 'send(stcp)',
                #              match='- Write Message To Socket'),
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.RECEIVE, 'receive_regex_match',
                #              regex=r'.* - BytesRead\((?P<receive_regex_match>.*)\) Occurred.*'
                #              ),
                #
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.SEND, 'send(tcp)',
                #              match='- Send TCP Message'),
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.SEND, 'send(udp)',
                #              match='- Send UDP Message'),
                # MessageEntry(Category.NETWORK, Source.CLIENT, Direction.SEND, 'send(mcast)',
                #              match='- Send Multicast Message')
                ]

    # TODO: Add check for duplicate labels

    @classmethod
    def lookup(cls, label) -> MessageEntry:
        return list(filter(lambda x: x.label == label, cls._entries))[0]

    @classmethod
    def by_source(cls, source: Source):
        return list(filter(lambda x: x.source == source, cls._entries))


class MessageInstance:
    def __init__(self, message_type: MessageEntry, value: str, label_override: str = None, timestamp: str = None):
        self._type = message_type
        self._value = value
        self._label = None if label_override is None else label_override
        if timestamp is None:
            self._timestamp = re.match(RE_TIMESTAMP, value).group()
        else:
            self._timestamp = timestamp

    def __str__(self) -> str:
        return self._timestamp + ': ' + str(self._type)

    @property
    def category(self) -> Category:
        return self._type.category

    @property
    def source(self) -> Source:
        return self._type.source

    @property
    def direction(self) -> Direction:
        return self._type.direction

    @property
    def label(self) -> str:
        return self._type.label if self._label is None else self._label

    @property
    def ignore(self) -> bool:
        return self._type.ignore

    @property
    def value(self):
        return self._value

    @property
    def timestamp(self):
        return self._timestamp

    @property
    def timestamp_ms(self):
        return get_time_ms(self.timestamp)

    def to_line(self) -> str:
        timestamp = re.match(RE_TIMESTAMP, self.value).group()
        return (
                timestamp + SEP + self.category.name + SEP + self.source.name + SEP +
                self.direction.name + SEP + self.label + SEP + self.value.replace(timestamp, '')
        )

    @property
    def sort_string(self) -> str:
        return self.timestamp + ('0' if self.direction == Direction.SEND else '1')

    @classmethod
    def from_line(cls, line: str):
        msg = line.split(SEP)
        timestamp = msg[0]
        # category = Category[msg[1]]
        # source = Source[msg[2]]
        # direction = Direction[msg[3]]
        label = msg[4]
        value = msg[5]

        return cls(MessageEntries.lookup(label), value, timestamp)


def split_path_name_extension(filepath: str) -> Tuple[str, str, str]:
    path, filename = os.path.split(filepath)
    name, ext = os.path.splitext(filename)
    return path, name, ext


def partition_lines(src: List[str], source: Source) -> Tuple[Dict[str, List[MessageInstance]], List[str]]:
    result = dict()  # type: Dict[str, List[MessageInstance]]
    remainder = list()

    message_entries = MessageEntries.by_source(source)

    for txt in src:
        line_placed = False

        for entry in message_entries:
            category = entry.category.value
            if line_placed:
                break

            if entry.regex is not None:
                re_match = re.match(entry.regex, txt)
                if re_match is not None:
                    if entry.ignore:
                        line_placed = True
                        break
                    else:
                        gd = re_match.groupdict()
                        if gd is not None and entry.label in gd:
                            message_instance = MessageInstance(entry, txt, label_override=gd[entry.label])
                        else:
                            message_instance = MessageInstance(entry, txt)

                        if category not in result.keys():
                            result[category] = list()
                        result[category].append(message_instance)
                        line_placed = True
                        break

            elif entry.match is not None:
                if entry.match in txt:
                    message_instance = MessageInstance(entry, txt)

                    if category not in result.keys():
                        result[category] = list()

                    result[category].append(message_instance)
                    line_placed = True
                    break

        if not line_placed:
            if txt.endswith('\n'):
                remainder.append(txt)
            else:
                remainder.append(txt + '\n')

    return result, remainder


def merge_category_round_trip(category_name: str, message_instances: List[MessageInstance], target_directory: str,
                              formatting: Formatting = Formatting.LOG):
    message_instances.sort(key=lambda x: x.sort_string)
    if formatting == Formatting.LOG:
        target_file = os.path.join(target_directory, category_name + '-' + formatting.value + '-roundTrip.txt')
        with open(target_file, 'w') as target:
            for message_instance in message_instances:
                if message_instance.source is not Source.SERVER:
                    target.write(message_instance.to_line())

    elif formatting == Formatting.TIMINGS:

        transactions_list = list()  # type: List[List[MessageInstance]]
        current_transaction_list = list()  # type: List[MessageInstance]

        seen_tx_counter = -1

        for message in message_instances:
            seen_tx_counter = seen_tx_counter + 1
            if len(transactions_list) == 0 and message.direction == Direction.RECEIVE:
                continue
            else:

                if message.source == Source.CLIENT:
                    if message.direction == Direction.SEND:
                        current_transaction_list = list()
                        transactions_list.append(current_transaction_list)
                        current_transaction_list.append(message)

                    elif message.direction == Direction.RECEIVE:
                        assert len(current_transaction_list) >= 1
                        assert current_transaction_list[0].source == Source.CLIENT
                        assert current_transaction_list[0].direction == Direction.SEND
                        current_transaction_list.append(message)

                    elif (message.direction == Direction.AUTH or message.direction == Direction.CONNECT
                          or message.direction == Direction.NONE):
                        pass

                    else:
                        raise Exception('Unexpected direction "' + message.direction.value + '!')
                    pass

                elif message.source == Source.SERVER:
                    pass

                else:
                    raise Exception('Unexpected source "' + message.source.value + '!')

        no_recipient_count = 0
        target_file = os.path.join(target_directory, category_name + '-' + formatting.value + '-roundTrip.txt')

        round_trip_timings = list()

        with open(target_file, 'w') as target:
            for transaction_list in transactions_list:  # type: List[MessageInstance]
                initial_client_tx = None
                first_client_rx = None
                last_client_rx = None

                for transaction in transaction_list:  # type: MessageInstance
                    if transaction.source == Source.CLIENT:
                        if transaction.direction == Direction.SEND:
                            assert initial_client_tx is None
                            initial_client_tx = transaction.timestamp_ms

                        elif transaction.direction == Direction.RECEIVE:
                            if first_client_rx is None:
                                first_client_rx = transaction.timestamp_ms

                            last_client_rx = transaction.timestamp_ms

                        else:
                            raise Exception('Unexpected direction "' + transaction.direction.value + '"!')

                    elif transaction.source == Source.SERVER:
                        pass
                    else:
                        raise Exception('Unexpected source "' + transaction.source.value + '"!')

                if first_client_rx is None or last_client_rx is None:
                    no_recipient_count = no_recipient_count + 1
                else:
                    duration = last_client_rx - initial_client_tx
                    round_trip_timings.append(duration)

                    target.write('client -> ' + (str(duration) + ' ms').rjust(8) + ' -> client\n')

        print(str(round_trip_timings))
        # exit(0)

        chart = Line()
        chart = StackedBar()
        chart.add('Timings', round_trip_timings)
        # chart.title = "Client Round Trip Timings"
        # chart.add('Client TX', client_tx_timings)
        # chart.add('Server Activity', server_activity_timings)
        # chart.add('Client RX', client_rx_timings)
        target_chart = os.path.join(target_directory, category_name + '-' + formatting.value + '-roundTrip.png')
        chart.render_to_png(target_chart)

        print('No recipients for ' + str(no_recipient_count) + ' transmissions')

    else:
        raise Exception('Unexpected formatting "' + str(formatting) + '"!')


def merge_category(category_name: str, message_instances: List[MessageInstance], target_directory: str,
                   formatting: Formatting = Formatting.LOG):
    message_instances.sort(key=lambda x: x.sort_string)
    if formatting == Formatting.LOG:
        target_file = os.path.join(target_directory, category_name + '-' + formatting.value + '-combined.txt')
        with open(target_file, 'w') as target:
            for message_instance in message_instances:
                target.write(message_instance.to_line())

    elif formatting == Formatting.TIMINGS:

        transactions_list = list()  # type: List[List[MessageInstance]]
        current_transaction_list = list()  # type: List[MessageInstance]

        seen_tx_counter = -1

        for message in message_instances:
            seen_tx_counter = seen_tx_counter + 1
            if len(transactions_list) == 0 and message.direction == Direction.RECEIVE:
                continue
            else:

                if message.source == Source.CLIENT:

                    if message.direction == Direction.SEND:
                        current_transaction_list = list()
                        transactions_list.append(current_transaction_list)
                        current_transaction_list.append(message)

                    elif message.direction == Direction.RECEIVE:
                        assert len(current_transaction_list) >= 3
                        assert current_transaction_list[0].source == Source.CLIENT
                        assert current_transaction_list[0].direction == Direction.SEND
                        assert current_transaction_list[1].source == Source.SERVER
                        assert current_transaction_list[1].direction == Direction.RECEIVE
                        current_transaction_list.append(message)

                    elif (message.direction == Direction.AUTH or message.direction == Direction.CONNECT
                          or message.direction == Direction.NONE):
                        pass

                    else:
                        raise Exception('Unexpected direction "' + message.direction.value + '!')
                    pass

                elif message.source == Source.SERVER:

                    if message.direction == Direction.RECEIVE:
                        assert len(current_transaction_list) == 1
                        assert (current_transaction_list[0])
                        current_transaction_list.append(message)

                    elif message.direction == Direction.SEND:
                        assert len(current_transaction_list) >= 2
                        assert current_transaction_list[0].source == Source.CLIENT
                        assert current_transaction_list[0].direction == Direction.SEND
                        assert current_transaction_list[1].source == Source.SERVER
                        assert current_transaction_list[1].direction == Direction.RECEIVE
                        current_transaction_list.append(message)

                    else:
                        raise Exception('Unexpected direction "' + message.direction.value + '!')
                    pass

                else:
                    raise Exception('Unexpected source "' + message.source.value + '!')

        no_recipient_count = 0
        target_file = os.path.join(target_directory, category_name + '-' + formatting.value + '-combined.txt')

        client_tx_timings = list()
        server_activity_timings = list()
        client_rx_timings = list()

        with open(target_file, 'w') as target:
            for transaction_list in transactions_list:  # type: List[MessageInstance]
                initial_client_tx = None
                initial_server_rx = None
                first_server_tx = None
                last_server_tx = None
                first_client_rx = None
                last_client_rx = None

                for transaction in transaction_list:  # type: MessageInstance

                    if transaction.source == Source.CLIENT:

                        if transaction.direction == Direction.SEND:
                            assert initial_client_tx is None
                            initial_client_tx = transaction.timestamp_ms

                        elif transaction.direction == Direction.RECEIVE:
                            if first_client_rx is None:
                                first_client_rx = transaction.timestamp_ms

                            last_client_rx = transaction.timestamp_ms

                        else:
                            raise Exception('Unexpected direction "' + transaction.direction.value + '"!')

                    elif transaction.source == Source.SERVER:

                        if transaction.direction == Direction.RECEIVE:
                            assert initial_client_tx is not None
                            assert initial_server_rx is None
                            initial_server_rx = transaction.timestamp_ms

                        elif transaction.direction == Direction.SEND:
                            if first_server_tx is None:
                                first_server_tx = transaction.timestamp_ms

                            last_server_tx = transaction.timestamp_ms
                        else:
                            raise Exception('Unexpected direction "' + transaction.direction.value + '"!')
                        pass

                    else:
                        raise Exception('Unexpected source "' + transaction.source.value + '"!')

                if first_server_tx is None or last_server_tx is None:
                    no_recipient_count = no_recipient_count + 1
                else:
                    client_tx_timing = initial_server_rx - initial_client_tx
                    client_tx_timings.append(client_tx_timing)
                    server_activity_timing = last_server_tx - initial_server_rx
                    server_activity_timings.append(server_activity_timing)
                    client_rx_timing = last_client_rx - first_server_tx
                    client_rx_timings.append(client_rx_timing)

                    target.write('client -> ' + (str(client_tx_timing) + ' ms').rjust(8) +
                                 ' -> server( ' + (str(server_activity_timing) + ' ms').rjust(8) + ' )' +
                                 ' -> ' + (str(client_rx_timing) + ' ms').rjust(8) + ' -> client\n')

        chart = StackedBar()
        chart.title = "Transaction Timings"
        chart.add('Client TX', client_tx_timings)
        chart.add('Server Activity', server_activity_timings)
        chart.add('Client RX', client_rx_timings)
        target_chart = os.path.join(target_directory, category_name + '-' + formatting.value + '-combined.png')
        chart.render_to_png(target_chart)

        print('No recipients for ' + str(no_recipient_count) + ' transmissions')

    else:
        raise Exception('Unexpected formatting "' + str(formatting) + '"!')


def parse_junit_xml_test_log(xml_log: str, target_directory: str = None) -> Tuple[str, str]:
    et = ElementTree.fromstring('\n'.join(open(xml_log, 'r')))
    source_path, source_name = os.path.split(xml_log)
    if target_directory is None:
        target_directory = source_path

    stdout_path = os.path.join(target_directory, source_name + '.stdout.log')
    stderr_path = os.path.join(target_directory, source_name + '.stderr.log')

    with open(stdout_path, 'w') as stdout:
        for line in et.find('system-out').text.split('\n'):
            if not line.strip() == '':
                stdout.write(line + os.linesep)

    with open(stderr_path, 'w') as stderr:
        txt = et.find('system-err').text
        if txt is not None:
            for line in txt.split('\n'):
                if not line.strip() == '':
                    stderr.write(line + os.linesep)

    return stdout_path, stderr_path


def main():
    args = _parser.parse_args()

    test_artifacts = args.test_artifacts
    output_directory = None if args.output_directory is None else os.path.abspath(args.output_directory)
    specific_test = args.specific_test
    round_trip_only = args.round_trip_only

    if not os.path.exists(test_artifacts):
        stderr.write('The test artifact does not exist!')
        exit(1)

    if os.path.isfile(test_artifacts) and test_artifacts.endswith('.zip'):
        zip_path = test_artifacts
        test_artifacts = test_artifacts[0:test_artifacts.rindex('.')]

        if os.path.exists(test_artifacts):
            stderr.write('The target zip extraction directory "' + test_artifacts + '" already exists!')
            exit(1)

        # os.mkdir(archive_directory)

        with ZipFile(zip_path) as zipper:
            zipper.extractall(test_artifacts)

        test_artifacts = os.path.join(test_artifacts, 'TEST_ARTIFACTS')

        if output_directory is None:
            output_directory = os.path.join(test_artifacts, 'results')

    elif os.path.isdir(test_artifacts):
        if output_directory is None:
            output_directory = os.path.join(test_artifacts, 'results')

    else:
        stderr.write('The --test-artifacts value must be a zip or a directory!')
        exit(1)

    # if archive_zip is not None:
    #     if not os.path.exists(archive_zip):
    #         stderr.write('The specified archive zip does not exist!')
    #         exit(1)
    #
    #     archive_directory = archive_zip[0:archive_zip.rindex('.')]
    #     if os.path.exists(archive_directory):
    #         stderr.write('The default results directory "' + archive_directory + '" already exists!')
    #         exit(1)
    #
    #     os.mkdir(archive_directory)
    #
    #     with ZipFile(archive_zip) as zipper:
    #         zipper.extractall(archive_directory)
    #
    #     if output_directory is None:
    #         output_directory = os.path.join(archive_directory, 'results')

    # if archive_directory is not None:
    #     if not os.path.exists(archive_directory):
    #         stderr.write('the archive directory "' + archive_directory + '" does not exist!')
    #         exit(1)

    # if output_directory is None:
    #     output_directory = os.path.join(archive_directory, 'results')

    if os.path.exists(output_directory):
        stderr.write('The target directory "' + output_directory + '" already exists!')
        exit(1)

    os.mkdir(output_directory)

    tdc = TestDataCollector(target_directory=output_directory, round_trip_only=round_trip_only)
    print('Parsing client logs to ' + output_directory + '...')
    tdc.load_xml_results(artifact_directory=test_artifacts, desired_test_identifier=specific_test)
    print('Parsing server logs to ' + output_directory + '...')
    tdc.load_server_logs(test_artifacts, desired_test_identifier=specific_test)

    print('Collating log contents in ' + output_directory + '...')
    tdc.produce_final_result()


if __name__ == '__main__':
    main()
