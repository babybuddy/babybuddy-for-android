import sys
import io

import panflute
import pypandoc

from dataclasses import dataclass, field
from typing import List, Optional

from xml.etree import ElementTree as ET

filename = sys.argv[1]
out_xml = sys.argv[2]

@dataclass
class Topic:
    title: str
    sections: List["Section"] = field(default_factory=list)

@dataclass
class Section:
    title: str
    image: Optional[str] = field(default=None)
    paragraphs: List[str] = field(default_factory=list)


class Filter:
    topics: List[Topic]

    def __init__(self):
        self.topics = []
    
    def filter(self, el: panflute.Element, doc):
        def extract_string(el: panflute.Element) -> str:
            parts = []
            def f(el, doc):
                if isinstance(el, panflute.Space):
                    parts.append(" ")
                elif isinstance(el, panflute.Str):
                    text = el.text
                    if isinstance(el.parent, panflute.Quoted):
                        text = f'"{text}"'
                    parts.append(text)
            el.walk(f)
            return "".join(parts)

        def last_section() -> Optional[Section]:
            if not self.topics:
                raise ValueError("No topic")
            if not self.topics[-1].sections:
                return None
            return self.topics[-1].sections[-1]

        if isinstance(el, panflute.Header):
            title = extract_string(el)
            if el.level == 1:
                self.topics.append(Topic(title))
            elif not self.topics:
                raise ValueError("No topic")
            else:
                self.topics[-1].sections.append(Section(title))
        elif isinstance(el, panflute.Para):
            sec = last_section()
            if not sec:
                return
            sec.paragraphs.append(extract_string(el))
        elif isinstance(el, panflute.Image):
            sec = last_section()
            if not sec:
                raise ValueError("Section for image required")
            if sec.image:
                raise ValueError("Only one image per section allowed")
            image: str = el.url.split("/")[-1]
            if "." in image:
                image = "".join(image.split(".")[:-1])
            sec.image = image
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
    el.text = content
    return el

help_xml = ET.fromstring("<resources />")
for topic_i, topic in enumerate(doc_data.topics):
    help_xml.append(ET.Comment(topic.title))
    for section_i, section in enumerate(topic.sections):
        item_id = f"help_item_{topic_i + 1}_{section_i + 1}"
        help_xml.append(string_el(f"{item_id}_title", section.title))
        help_xml.append(string_el(f"{item_id}_image", section.image or "none"))
        help_xml.append(string_el(f"{item_id}_text", "\n\n".join(section.paragraphs).strip()))
        
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

