import re

section_title = None
section_contents = []

output = []

def commit_current():
    global section_title, section_contents
    if section_title is None:
        return

    for line in section_contents:
        text_line = re.sub(r"<[^>]*>", "", line).strip()

        href = re.search(r'href="([^"]*)"', line).group(1).strip()
        author = re.search("created by ([^-]*)-", text_line).group(1).strip()
        _type = re.search("^(.*) created", text_line).group(1).strip()
        
        output.append({"author": author, "href": href, "type": _type})
    
    section_title = None
    section_contents = []


for line in open("ATTRIBUTIONS.md", "r"):
    if line.startswith("##"):
        commit_current()
        section_title = line[2:].strip()
    elif line.startswith("<a "):
        section_contents.append(line.strip())

commit_current()

# Group by author
per_author = {}
for o in output:
    per_author[o["author"]] = per_author.get(o["author"], []) + [o]


print("Third Party Attributions")
print("""
The project contains the icons from www.flaticon.com, made by the following authors:
""", end="")
for author, values in per_author.items():
    for v in values:
      items_list_text = "{type} ({href})".format(**v)
      print(f"- {author}: {items_list_text}")

