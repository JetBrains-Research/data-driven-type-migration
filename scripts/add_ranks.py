import json
from dataclasses import dataclass

from bs4 import BeautifulSoup

from logging_json_prepare import PATH_TO_TYPE_CHANGE_PATTERNS

PATH_TO_TYPE_CHANGE_PATTERNS_WITH_RANKS = '../plugin/src/main/resources/rules_with_ranks.json'


@dataclass
class PatternInfo:
    source_type: str
    target_type: str
    rank: int


def main():
    with open('data/TypeChangeSummary.html', 'r') as file:
        html = file.read()
    soup = BeautifulSoup(html, 'html.parser')
    data = []
    for tr in soup.table.find_all('tr'):
        tds = tr.find_all('td')
        if not tds:
            continue
        data.append(
            PatternInfo(
                source_type=tds[0].text.replace(':[v0]', '$1$').replace(':[v1]', '$2$'),
                target_type=tds[1].text.replace(':[v0]', '$1$').replace(':[v1]', '$2$'),
                rank=int(tds[4].text)
            )
        )
    data.sort(key=lambda pattern: pattern.rank, reverse=True)

    with open(PATH_TO_TYPE_CHANGE_PATTERNS, "r") as file:
        patterns = json.load(file)
    for pattern in patterns:
        for info in data:
            if info.source_type == pattern['From'] and info.target_type == pattern['To']:
                pattern['Rank'] = info.rank

    with open(PATH_TO_TYPE_CHANGE_PATTERNS_WITH_RANKS, "w") as file:
        json.dump(patterns, file)


if __name__ == '__main__':
    main()
