help:
	@echo "Targets:"
	@echo "  smaller - Regenerates all '-smaller' images"

smaller:
	ls *.png | grep -v -- "-smaller.png$$" > list_of_original_images
	cat list_of_original_images | sed 's/.png//'  | xargs -i -d'\n' -r   convert {}.png -scale 33% {}-smaller.png
	rm list_of_original_images
