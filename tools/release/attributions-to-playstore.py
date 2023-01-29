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


print("<h2><b>Third Party Attributions</b></h2>")
print("""
The application contains the following media from www.flaticon.com, licensed under their attributions license for free use:
""", end="")
for author, values in per_author.items():
    for v in values:
        locals().update(v)
        print(f"""- <a href="{href}">{type} created by {author} - Flaticon</a>""")

