import sys
import io

import panflute
import pypandoc

import dataclasses
from dataclasses import dataclass
from typing import List, Optional

from xml.etree import ElementTree as ET

filename = sys.argv[1]
out_xml = sys.argv[2]

@dataclass
class Library:
    title: str
    source: str = None
    copyright: str = None
    full_text: str = None


class Filter:
    libraries: List[Library]

    def __init__(self):
        self.libraries = []
    
    def filter(self, el: panflute.Element, doc):
        def extract_string(extract_el: panflute.Element) -> str:
            parts = []
            def f(el, doc):
                if isinstance(el, panflute.Quoted) and (extract_el != el):
                    parts.append('\\"{}\\"'.format(extract_string(el)))
                elif isinstance(el.parent, panflute.Quoted) and (extract_el != el.parent):
                    return
                elif isinstance(el, (panflute.LineBreak, panflute.SoftBreak, panflute.Space)):
                    parts.append(" ")
                elif isinstance(el, panflute.Str):
                    text = el.text
                    parts.append(text)
            extract_el.walk(f)
            return "".join(parts)

        def last_library() -> Library:
            if not self.libraries:
                raise ValueError("No library")
            return self.libraries[-1]

        if isinstance(el, panflute.Header):
            title = extract_string(el)
            print("Header level", el.level)
            if el.level == 2:
                self.libraries.append(Library(title))
        elif isinstance(el, panflute.Para) and extract_string(el).startswith("Source:"):
            last_library().source = ":".join(extract_string(el).split(":", 1)[1:])
        elif isinstance(el, panflute.Image):
            raise ValueError("Image not supported")
        elif isinstance(el, panflute.CodeBlock):
            lib = last_library()
            if lib.copyright is None:
                lib.copyright = el.text
            elif lib.full_text is None:
                lib.full_text = el.text
            else:
                raise ValueError("Extra unexpected code section")
        else:
            if el.parent != doc:
                return

            print(f"Warning, ignored: {el}")


doc_data = Filter()


data = pypandoc.convert_file(filename, 'json', format='markdown')
doc = panflute.load(io.StringIO(data))

panflute.run_filter(doc_data.filter, doc=doc)

def string_el(name: str, content: str) -> ET.Element:
    el = ET.Element("string")
    el.attrib["name"] = name
    el.text = '"{}"'.format(
        content.strip("\n").replace("'", "\\'").replace('"', '\\"')
    )
    return el

help_xml = ET.fromstring("<resources />")
for lib_i, lib in enumerate(doc_data.libraries):
    if None in dataclasses.astuple(lib):
        raise ValueError("Library '{}' misses these values: {}".format(
            lib.title,
            ", ".join(k for k, v in dataclasses.asdict(lib).items() if v is None),
        ))

    help_xml.append(string_el(f"lib_license_{lib_i}_title", lib.title))
    help_xml.append(string_el(f"lib_license_{lib_i}_source", lib.source))
    help_xml.append(string_el(f"lib_license_{lib_i}_copyright", lib.copyright))
    help_xml.append(string_el(f"lib_license_{lib_i}_full_text", lib.full_text))
        
# Pretty indentation
indent = "\n" + (" " * 4)
has_element = False
for i, el in enumerate(help_xml):
    has_element = True
    el.tail = indent

if has_element:
    help_xml.text = indent
    help_xml[-1].tail = "\n"

xml_text = '<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(help_xml).decode()
with open(out_xml, "w") as f:
    f.write(xml_text)
