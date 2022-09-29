#!/usr/bin/env python

import argparse
import os

from lxml import etree

parser = argparse.ArgumentParser('CoreConfig Configuration Helper')
parser.add_argument('source', metavar='SOURCE', type=str, help='The source CoreConfig path')
parser.add_argument('target', metavar='TARGET', type=str, help='The target CoreConfig path')


CORE_CONFIG_NAMESPACE = 'http://bbn.com/marti/xml/config'


class ConfigEntry:
    def __init__(self, env_var, xpath, attribute_name, required, hide_value):
        self._env_var = env_var  # type: str
        self._xpath = xpath  # type: str
        self._attribute_name = attribute_name  # type: str
        self._required = required  # type: bool
        self._hide_value = hide_value  # type: bool

    @property
    def attribute_name(self):
        return self._attribute_name

    @property
    def xpath(self):
        return self._xpath

    @property
    def env_var(self):
        return self._env_var

    @property
    def required(self):
        return self._required

    @property
    def hide_value(self):
        return self._hide_value

    def value(self):
        if self._env_var in os.environ.keys():
            return os.environ[self._env_var]
        else:
            return None


CONFIG_VALUES = [
        ConfigEntry('POSTGRES_URL', 'tak:repository/tak:connection', 'url', False, False),
        ConfigEntry('POSTGRES_USER', 'tak:repository/tak:connection', 'username', False, False),
        ConfigEntry('POSTGRES_PASSWORD', 'tak:repository/tak:connection', 'password', True, True),
        ConfigEntry('TAKSERVER_CERT_PASS', 'tak:security/tak:tls', 'keystorePass', True, True),
        ConfigEntry('CA_PASS', 'tak:security/tak:tls', 'truststorePass', True, True)
]


class CoreConfigHelper:
    def __init__(self, source_filepath):
        self._source_filepath = source_filepath
        self._tree = etree.parse(open(source_filepath), etree.XMLParser())
        self._root = self._tree.getroot()
        self._namespaces = {
            'tak': CORE_CONFIG_NAMESPACE
        }

    def find(self, xpath):
        """
        :rtype: etree.Element
        """
        results = self._tree.findall(path=xpath, namespaces=self._namespaces)

        if len(results) > 1:
            raise Exception('XPath expressions that return multiple elements are not currently supported!')
        return results[0]

    def process_configuration(self, config_values, target_filepath):
        """
        :type config_values: list[ConfigEntry]
        """
        for config in config_values:
            value = config.value()
            if value is None:
                if config.required:
                    raise Exception('The environment variable "' + config.env_var + '" is required!')
            else:
                element = self.find(config.xpath)
                element.set(config.attribute_name, value)
                if config.hide_value:
                    print(config.xpath.replace('tak:', '') + ' attribute ' + config.attribute_name + ' set to ********')
                else:
                    print(config.xpath.replace('tak:', '') + ' attribute ' + config.attribute_name + ' set to "' + value + '"')

        self._tree.write(target_filepath, xml_declaration=True, encoding='UTF-8')


def main():
    args = parser.parse_args()
    helper = CoreConfigHelper(args.source)
    helper.process_configuration(CONFIG_VALUES, args.target)


if __name__ == '__main__':
    main()
