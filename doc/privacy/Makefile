version := 1.1

css = babybuddy-privacy-1.css
templated_md = privacy.md-$(version)

all: babybuddy-privacy-v$(version).pdf babybuddy-privacy-v$(version).html

$(templated_md): privacy.md Makefile
	DOC_VERSION=$(version) envsubst '$$DOC_VERSION' < $< > $@

*.pdf: $(templated_md)
	pandoc $< --from=gfm+yaml_metadata_block -V linkcolor=blue -o $@

*.html: $(templated_md) $(css)
	pandoc $< --from=gfm+yaml_metadata_block --css $(css) -s -o $@
