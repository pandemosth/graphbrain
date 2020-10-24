import logging
import re
import csv
import sys
import progressbar
from graphbrain.cognition.agent import Agent
from graphbrain.op import create_op


def file_lines(filename):
    with open(filename, 'r') as f:
        for i, _ in enumerate(f, 1):
            pass
    return i


def text_parts(title):
    parts = re.split('\|| - | -- |^\[([^\]]*)\] | \[([^\]]*)\]$', title)
    parts = [part.strip() for part in parts if part]
    return parts


class CsvParser(Agent):
    def __init__(self, name, progress_bar=True, logging_level=logging.INFO):
        super().__init__(
            name, progress_bar=progress_bar, logging_level=logging_level)
        # TODO: make this configurable
        self.text = 'title'
        self.parser = None

    def on_start(self):
        csv.field_size_limit(sys.maxsize)

    def _parse_row(self, row):
        parser = self.system.get_parser(self)

        parts = text_parts(row[self.text])

        for part in parts:
            parse_results = parser.parse(part)
            for parse in parse_results['parses']:
                main_edge = parse['main_edge']

                # add main edge
                if main_edge:
                    # attach text to edge
                    text = parse['text']
                    attr = {'text': text}
                    yield create_op(main_edge, attributes=attr)

                    # add extra edges
                    for edge in parse['extra_edges']:
                        yield create_op(edge)

    def run(self):
        infile = self.system.get_infile(self)

        lines = file_lines(infile) - 1
        i = 0
        with progressbar.ProgressBar(max_value=lines) as bar:
            with open(infile, 'r') as f:
                csv_reader = csv.DictReader(f)
                for row in csv_reader:
                    for wedge in self._parse_row(row):
                        yield wedge
                    i += 1
                    bar.update(i)