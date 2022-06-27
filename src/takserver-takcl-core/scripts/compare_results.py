#!/usr/bin/env python3

import argparse
import json
import os

parser = argparse.ArgumentParser('Compares the results of two integration test run "integration_test_results" directories.')
parser.add_argument('--model-result', metavar='MODEL_RESULT', required=True,
                    help='The desired model result that is known to be good')
parser.add_argument('--test-result', metavar='TEST_RESULT', required=True,
                    help='The result that should be tested against the model result.')


def main():
    # Set these before running
    model_result = None
    test_result = None

    model_files = dict()
    test_files = dict()

    for path, directories, files in os.walk(model_result):
        for file in files:
            if file.endswith('.json'):
                model_files[file] = path

    for path, directories, files in os.walk(test_result):
        for file in files:
            if file.endswith('.json'):
                test_files[file] = path

    model_key_set = set(model_files.keys())
    test_key_set = set(test_files.keys())

    new_tests = sorted(test_key_set.difference(model_key_set))
    missing_tests = sorted(model_key_set.difference(test_key_set))
    common_tests = sorted(model_key_set.intersection(test_key_set))

    print('Missing Tests:\n\t' + '\n\t'.join(missing_tests))
    print('New Tests:\n\t' + '\n\t'.join(new_tests))
    # print('Common Tests:\n\t' + '\n\t'.join(common_tests))

    equal_files = list()
    different_files = list()

    for test_file in common_tests:
        model_filepath = os.path.join(model_files[test_file], test_file)
        test_filepath = os.path.join(test_files[test_file], test_file)

        model_dict = json.load(open(model_filepath))
        test_dict = json.load(open(test_filepath))

        if model_dict == test_dict:
            equal_files.append(test_file)
        else:
            different_files.append(test_file)

    print('Valid Tests:\n\t' + '\n\t'.join(equal_files))
    print('Invalid Tests:\n\t' + '\n\t'.join(different_files))







if __name__ == '__main__':
    main()
