version:=1.1

all:
	pandoc privacy.md --from=gfm+yaml_metadata_block -V linkcolor=blue -o babybuddy-privacy-v$(version).pdf
	pandoc privacy.md --from=gfm+yaml_metadata_block --css babybuddy-privacy-1.css -s -o babybuddy-privacy-v$(version).html
