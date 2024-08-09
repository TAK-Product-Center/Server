import boto3

from datetime import datetime, timedelta
import logging
from pprint import pprint
import random
import time
from botocore.exceptions import ClientError
import multiprocessing

logger = logging.getLogger(__name__)

class CloudWatchWrapper:
    """Encapsulates Amazon CloudWatch functions."""
    def __init__(self, cloudwatch_resource):
        """
        :param cloudwatch_resource: A Boto3 CloudWatch resource.
        """
        self.cloudwatch_resource = cloudwatch_resource


    def list_metrics(self, namespace, name, recent=False):
        """
        Gets the metrics within a namespace that have the specified name.
        If the metric has no dimensions, a single metric is returned.
        Otherwise, metrics for all dimensions are returned.

        :param namespace: The namespace of the metric.
        :param name: The name of the metric.
        :param recent: When True, only metrics that have been active in the last
                       three hours are returned.
        :return: An iterator that yields the retrieved metrics.
        """
        try:
            kwargs = {'Namespace': namespace, 'MetricName': name}
            if recent:
                kwargs['RecentlyActive'] = 'PT3H'  # List past 3 hours only
            metric_iter = self.cloudwatch_resource.metrics.filter(**kwargs)
            logger.info("Got metrics for %s.%s.", namespace, name)
        except ClientError:
            logger.exception("Couldn't get metrics for %s.%s.", namespace, name)
            raise
        else:
            return metric_iter

    def put_metric_data(self, namespace, uid, name, value, unit):
        """
        Sends a single data value to CloudWatch for a metric. This metric is given
        a timestamp of the current UTC time.

        :param namespace: The namespace of the metric.
        :param name: The name of the metric.
        :param value: The value of the metric.
        :param unit: The unit of the metric.
        """
        try:
            metric = self.cloudwatch_resource.Metric(namespace, name)
            metric.put_data(
                Namespace=namespace,
                MetricData=[{
                    'Dimensions': [{'Name': 'uid','Value': uid}],
                    'MetricName': name,
                    'Value': value,
                    'Unit': unit
                }]
            )
            logger.info("Put data for metric %s.%s", namespace, name)
        except ClientError:
            logger.exception("Couldn't put data for metric %s.%s", namespace, name)
            raise

    def put_metric_pyTAK_stats(self, namespace, uid, stats):

        try:
            self.put_metric_data(namespace=namespace, uid = uid, name='messages_sent', value=stats['messages_sent'], unit='Count')
            self.put_metric_data(namespace=namespace, uid = uid, name='messages_received', value=stats['messages_received'], unit='Count')
            self.put_metric_data(namespace=namespace, uid = uid, name='connect_event_count', value=stats['connect_event_count'], unit='Count')
            self.put_metric_data(namespace=namespace, uid = uid, name='disconnect_event_count', value=stats['disconnect_event_count'], unit='Count')
            self.put_metric_data(namespace=namespace, uid = uid, name='ping_count', value=stats['ping_count'], unit='Count')
            self.put_metric_data(namespace=namespace, uid = uid, name='pong_count', value=stats['pong_count'], unit='Count')
            self.put_metric_data(namespace=namespace, uid = uid, name='time_between_ping_pong', value=stats['time_between_ping_pong'], unit='Milliseconds')

            logger.info("Done sending pyTAK stats data to namespace %s", namespace)
            # print(f"Done sending pyTAK stats data to namespace {namespace}")
        except ClientError:
            logger.exception("Couldn't put data set for metric %s.", namespace)
            raise


def cloud_watch_process(uid, arr, metric_namespace, send_metrics_interval):

    print(f"Starting a cloud_watch_process for uid {uid}. Will send metrics to namespace: {metric_namespace} every {send_metrics_interval} seconds")

    cw_wrapper = CloudWatchWrapper(boto3.resource('cloudwatch'))

    while True:
        time.sleep(send_metrics_interval) 
        try:
            stats_for_uid = {
                'messages_sent': arr[0],
                'messages_received': arr[1],
                'connect_event_count': arr[2],
                'disconnect_event_count': arr[3],
                'ping_count': arr[4],
                'pong_count': arr[5],
                'time_between_ping_pong': arr[6]
            }

            print(f"Putting pyTAK stat data for uid {uid} into metric namespace: {metric_namespace}, stats: {stats_for_uid}")

            cw_wrapper.put_metric_pyTAK_stats(metric_namespace, uid, stats_for_uid)

        except KeyboardInterrupt:
            logger.info("KeyboardInterrupt")
            break
        except:
            logger.exception("Error occurs when pushing stats data to CloudWatch")
        finally:
            # reset stats value after sending to cloud_watch
            for i in range(len(arr)):
                arr[i] = 0
                
    print("cloud_watch_process has stopped")


def print_info_without_sending_to_cloud_watch_process(uid, arr, send_metrics_interval):

    while True:
        time.sleep(send_metrics_interval) 
        try:
            stats_for_uid = {
                'messages_sent': arr[0],
                'messages_received': arr[1],
                'connect_event_count': arr[2],
                'disconnect_event_count': arr[3],
                'ping_count': arr[4],
                'pong_count': arr[5],
                'time_between_ping_pong': arr[6]
            }

            print(f"Info that would be sent to CloudWatch if it was enabled, uid: {uid}, stats: {stats_for_uid}")

        except KeyboardInterrupt:
            logger.info("KeyboardInterrupt")
            break
        finally:
            # reset stats value after sending to cloud_watch
            for i in range(len(arr)):
                arr[i] = 0
                

# This main method below can be used to test CloudWatch configuration on ~/.aws/credentials and ~/.aws/config
if __name__ == "__main__":

    print("-"*88)

    logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')

    cw_wrapper = CloudWatchWrapper(boto3.resource('cloudwatch'))
    metric_namespace = 'doc-example-metric'
    print(f"Putting pyTAK stat data into metric namespace {metric_namespace}")

    cw_wrapper.put_metric_pyTAK_stats(
        metric_namespace, "uid_temp_123",
        {
            'messages_sent': 10,
            'messages_received': 20,
            'connect_event_count': 1,
            'disconnect_event_count': 0,
            'ping_count': 6,
            'pong_count': 5,
            'time_between_ping_pong': 40
        })

    print("-"*88)
    metric_iter = cw_wrapper.list_metrics(metric_namespace, 'messages_sent')
    for metric in metric_iter:
        print(f"metric: {metric}")
    print("Done")

