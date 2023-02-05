section_title = None
section_contents = []

output = []

def commit_current():
    global section_title, section_contents
    if section_title is None:
        return
    
    output.append(f"- {section_title}: " + ", ".join(section_contents))
    
    section_title = None
    section_contents = []


for line in open("ATTRIBUTIONS.md", "r"):
    if line.startswith("##"):
        commit_current()
        section_title = line[2:].strip()
    elif line.startswith("<a "):
        section_contents.append(line.strip())

commit_current()

print("# Third party media")
print("""
The project contains the icons from [flaticon.com](flaticon.com), used under the free license:
""")
for line in output:
    print(line)

