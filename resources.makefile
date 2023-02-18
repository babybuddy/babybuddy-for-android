.PHONY: all info refresh-flaticon-token help

SPACE := $(null) $(null)

FREE_IMAGES = \
	resources/feeding-breast.png::pkg_breast

FLATICON_IMAGES = \
	6056851?size=512::pkg_crawl::-gravity_center_-resize_90%_-extent_512x512:: \
	768140?size=512::pkg_diaper::-gravity_center_-resize_90%_-extent_512x512:: \
	865779?size=512::pkg_sleep::-gravity_center_-resize_90%_-extent_512x512:: \
	5834939?size=512::pkg_bottle::-gravity_center_-resize_90%_-extent_512x512:: \
	2128614?size=512::pkg_solid_food::-gravity_center_-resize_90%_-extent_512x512:: \
	4063767?size=512::pkg_poop::-gravity_center_-resize_100%_-extent_512x512:: \
	3313952?size=512::pkg_wet::-gravity_center_-resize_80%_-extent_512x512:: \
	1001371?size=512::pkg_notes::-gravity_center_-resize_90%_-extent_512x512:: \
	4063767?size=512::pkg_no_poop::-gravity_center_-resize_100%_-extent_512x512::resources/intermed_cross.png \
	3313952?size=512::pkg_not_wet::-gravity_center_-resize_80%_-extent_512x512::resources/intermed_cross.png \
	1001371?size=512::pkg_no_notes::-gravity_center_-resize_90%_-extent_512x512::resources/intermed_cross.png \

# Replace again!
# 1001371 -> 768818??size=512::pkg_notes::-gravity_center_-extent_512x512 \


DRAWABLE_VARIANTS = \
	drawable-mdpi::24 \
	drawable-hdpi::36 \
	drawable-xhdpi::48 \
	drawable-xxhdpi::72 \
	drawable-xxxhdpi::96 \

MIPMAPS = \
	mipmap-mdpi::48 \
	mipmap-hdpi::72 \
	mipmap-xhdpi::96 \
	mipmap-xxhdpi::144 \
	mipmap-xxxhdpi::192 \

VARIANTS_BASE_PATH := app/src/main/res/

# Func: get_field
#   Syntax: get_field,index,::-separated-list
# 
#  index:      1-based index into the ::-separted-list
#  ::-separated-list:
#              list::of::items::separated::by::double::colons
get_field = $(word $(1),$(subst ::,$(SPACE),$(2)))

DRAWABLE_TARGETS = \
    $(foreach x,$(FLATICON_IMAGES),$(foreach v,$(DRAWABLE_VARIANTS), $(VARIANTS_BASE_PATH)$(call get_field,1,$(v))/$(call get_field,2,$(x)).png)) \
    $(foreach x,$(FREE_IMAGES),$(foreach v,$(DRAWABLE_VARIANTS), $(VARIANTS_BASE_PATH)$(call get_field,1,$(v))/$(call get_field,2,$(x)).png))

PROGRAM_ICON_TARGETS = \
    $(foreach v,$(MIPMAPS), $(VARIANTS_BASE_PATH)$(call get_field,1,$(v))/ic_launcher.png) \
    $(foreach v,$(MIPMAPS), $(VARIANTS_BASE_PATH)$(call get_field,1,$(v))/ic_launcher_round.png) \
    app/src/main/pkg_app_icon-playstore.png

ALL_TARGETS = \
    $(DRAWABLE_TARGETS) \
    $(PROGRAM_ICON_TARGETS) \

all: $(ALL_TARGETS) help

help:
	$(MAKE) -f help.makefile all

info:
	@echo -e -- Targets refreshed by this command: \\n  $(subst $(SPACE)$(SPACE),\\n$(SPACE)$(SPACE),$(ALL_TARGETS))

# Generate drawable variants for android
define _drawable_variants_macro =

$(VARIANTS_BASE_PATH)$$(call get_field,1,$(1))/%.png: resources/%.png
	convert "$$<" -resize "$$(call get_field,2,$(1))x$$(call get_field,2,$(1))" "$$@"

$(VARIANTS_BASE_PATH)$$(call get_field,1,$(1))/%.png: resources/nonfree/%.png
	convert "$$<" -resize "$$(call get_field,2,$(1))x$$(call get_field,2,$(1))" "$$@"

$(VARIANTS_BASE_PATH)$$(call get_field,1,$(1))/%.png: resources/tmp/%.png
	convert "$$<" -resize "$$(call get_field,2,$(1))x$$(call get_field,2,$(1))" "$$@"

endef

$(foreach v,$(DRAWABLE_VARIANTS),$(eval $(call _drawable_variants_macro,$(v))))


# Free images that are part of the repository
define _prepare_free_image =

resources/tmp/$$(call get_field,2,$(1)).png: $$(call get_field,1,$(1))
	cp "$$<" "$$@"

endef

$(foreach i,$(FREE_IMAGES),$(eval $(call _prepare_free_image,$(i))))


# Flaticon images
flaticon-apikey:
	[ -e flaticon-apikey ] || ( echo "You need to create a flaticon api key first" && exit 1 )

refresh-flaticon-token: flaticon-apikey
	@bash utils/test_flaticontoken.sh ./flaticon-token && \
	echo "Flaticon token still valid" || \
	( \
		curl $$CURL_ARGS -X POST -H "Accept: application/json" --data apikey=$$( cat ./flaticon-apikey ) https://api.flaticon.com/v3/app/authentication | jq -r ".data.token" > ./flaticon-token && \
		echo Flaticon token refreshed \
	)

flaticon_targets_created :=
define _flaticon_image =

image_id := $$(call get_field,1,$$(subst ?,::,$$(call get_field,1,$(1))))

resources/nonfree/$$(call get_field,2,$(1)).marker: refresh-flaticon-token
	@echo "$(1)" > "$$@.tmp"
	@( [ -e "$$@" ] && diff "$$@" "$$@.tmp" > /dev/null ) && rm "$$@.tmp" || mv "$$@.tmp" "$$@"

ifeq (,$$(findstring $$(image_id),$$(flaticon_targets_created)))
flaticon_targets_created += $$(image_id)
resources/nonfree/raw_$$(image_id).png:
	curl $$$$CURL_ARGS -X GET \
		-H "Accept: application/json" \
		-H "Authorization: Bearer $$$$( cat ./flaticon-token )" \
		--output "$$@" \
		"https://api.flaticon.com/v2/item/icon/download/$$(call get_field,1,$(1))"
endif

resources/nonfree/$$(call get_field,2,$(1)).png: resources/nonfree/raw_$$(image_id).png resources/nonfree/$$(call get_field,2,$(1)).marker
	convert "$$<" $$(subst $$(SPACE)$$(SPACE),_,$$(subst _,$(SPACE),$$(call get_field,3,$(1)))) "$$@.tmp.png"
	convert "$$@.tmp.png" +clone -alpha off -compose Copy__Opacity -composite -channel A -negate "$$@"
	[ -z "$$(strip $$(call get_field,4,$(1)))" ] || ( cp "$$@" "$$@.tmp.png" && composite "$$@.tmp.png" "$$(call get_field,4,$(1))" "$$@" )
	rm "$$@.tmp.png"
	
endef

$(foreach flaticon,$(FLATICON_IMAGES), $(eval $(call _flaticon_image,$(flaticon))))

# Rectangular icon conversion
define _convert_rectangular_icon =

$(VARIANTS_BASE_PATH)$$(call get_field,1,$(1))/ic_launcher.png: resources/icon/icon.png
	mkdir $$(dir $$@) 2> /dev/null || true
	convert $$< -resize $$(call get_field,2,$(1))x$$(call get_field,2,$(1)) $$@

endef

$(foreach mipmap,$(MIPMAPS), $(eval $(call _convert_rectangular_icon,$(mipmap))))

# Round icon conversion
ROUND_ICON_CUTOUT = resources/tmp/_round_icon_cutout.png

$(ROUND_ICON_CUTOUT): resources/icon/round_icon_cutout.svg
	convert -background none $< -resize 512x512 $@

define _convert_rectangular_icon =

$(VARIANTS_BASE_PATH)$$(call get_field,1,$(1))/ic_launcher_round.png: resources/icon/icon.png $(ROUND_ICON_CUTOUT)
	mkdir $$(dir $$@) 2> /dev/null || true
	convert -background none $$< -gravity center -scale 80% -extent 512x512 $(ROUND_ICON_CUTOUT) -compose Multiply -composite -resize $$(call get_field,2,$(1))x$$(call get_field,2,$(1)) $$@

endef

$(foreach mipmap,$(MIPMAPS), $(eval $(call _convert_rectangular_icon,$(mipmap))))

# Playstore icon
app/src/main/pkg_app_icon-playstore.png: resources/icon/icon.png
	convert -background none $< -resize 512x512 $@
