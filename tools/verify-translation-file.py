import sys
import argparse

from dataclasses import dataclass
import xml.etree.ElementTree as ET

@dataclass
class StringsXmlContent:
    strings: dict[str, str]
    arrays: dict[str, list[str]]

    def iter_all_strings(self):
        for key, value in self.strings.items():
            yield key, value
        for key, values in self.arrays.items():
            for i, value in enumerate(values):
                yield f"{key}[{i}]", value

def parse_strings_xml(file_path) -> StringsXmlContent:
    tree = ET.parse(file_path)
    root = tree.getroot()

    strings = {}
    for string_element in root.iter('string'):
        if string_element.attrib.get('translatable', '').strip().lower() == 'false':
            continue

        key = string_element.get('name')
        value = string_element.text
        strings[key] = value


    arrays = {}
    for array_element in root.iter('array'):
        if array_element.attrib.get('translatable', '').strip().lower() == 'false':
            continue

        key = array_element.get('name')
        value = []
        for item in array_element.iter('item'):
            value.append(item.text)
        arrays[key] = value

    return StringsXmlContent(strings, arrays)

def collect_phrase_placeholders(s: str) -> set[str]:
    """
    Collect all placeholders that are used for the Java Phrase library.
    """
    placeholders = set()
    start = 0
    while start < len(s):
        start = s.find("{", start)
        if start == -1:
            break
        end = s.find("}", start)
        if end == -1:
            break
        placeholders.add(s[start+1:end])
        start = end
    return placeholders

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Verify translation file")
    parser.add_argument("reference_strings_xml", help="Reference strings.xml file")
    parser.add_argument("translated_strings_xml", help="Translated strings.xml file")
    args = parser.parse_args()

    reference_strings = parse_strings_xml(args.reference_strings_xml)
    translated_strings = parse_strings_xml(args.translated_strings_xml)

    reference_placeholders = {
        key: collect_phrase_placeholders(value)
        for key, value in reference_strings.iter_all_strings()
    }
    translated_strings = {
        key: collect_phrase_placeholders(value)
        for key, value in translated_strings.iter_all_strings()
    }

    failed = False

    # list divergent keys
    keys_equal = set(reference_placeholders.keys()) == set(translated_strings.keys())
    if not keys_equal:
        failed = True
        print("Missing keys:")
        for k in reference_placeholders.keys() - translated_strings.keys():
            print("- ", k)
        print("Invalid translation keys:")
        for k in translated_strings.keys() - reference_placeholders.keys():
            print("- ", k)
    
    # list divergent placeholders
    print("Divergent phrase placeholders:")
    for key in reference_placeholders.keys() & translated_strings.keys():
        if reference_placeholders[key] != translated_strings[key]:
            failed = True
            print(f"Phrase placeholders for key '{key}' diverge:")
            print("Reference:", reference_placeholders[key])
            print("Translated:", translated_strings[key])
            print()

    if failed:
        sys.exit(1)
    else:
        print("All checks passed.")
        sys.exit(0)
