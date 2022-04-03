SPACE := $(null) $(null)

FREE_IMAGES = \
	feeding-breast.png::pkg_breast

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


VARIANTS = \
	drawable-mdpi::24 \
	drawable-hdpi::36 \
	drawable-xhdpi::48 \
	drawable-xxhdpi::72 \
	drawable-xxxhdpi::96 \

VARIANTS_BASE_PATH := app/src/main/res/

get_field = $(word $(1),$(subst ::,$(SPACE),$(2)))

ALL_TARGETS = $(foreach x,$(FLATICON_IMAGES),$(foreach v,$(VARIANTS), $(VARIANTS_BASE_PATH)$(call get_field,1,$(v))/$(call get_field,2,$(x)).png))

.PHONY: all refresh-flaticon-token

all: $(ALL_TARGETS)

flaticon-apikey:
	[ -e flaticon-apikey ] || ( echo "You need to create a flaticon api key first" && exit 1 )

refresh-flaticon-token: flaticon-apikey
	@bash utils/test_flaticontoken.sh ./flaticon-token && \
	echo "Flaticon token still valid" || \
	( \
		curl $$CURL_ARGS -X POST -H "Accept: application/json" --data apikey=$$( cat ./flaticon-apikey ) https://api.flaticon.com/v3/app/authentication | jq -r ".data.token" > ./flaticon-token && \
		echo Flaticon token refreshed \
	)

# Generate variant pngs
define _variants_macro =

$(VARIANTS_BASE_PATH)$$(call get_field,1,$(1))/%.png: resources/nonfree/%.png
	convert "$$^" -resize "$$(call get_field,2,$(1))x$$(call get_field,2,$(1))" "$$@"

endef

$(foreach v,$(VARIANTS),$(eval $(call _variants_macro,$(v))))

# Download images

targets_created :=
define _flaticon_image =

image_id := $$(call get_field,1,$$(subst ?,::,$$(call get_field,1,$(1))))

resources/nonfree/$$(call get_field,2,$(1)).marker: refresh-flaticon-token
	@echo "$(1)" > "$$@.tmp"
	@( [ -e "$$@" ] && diff "$$@" "$$@.tmp" > /dev/null ) && rm "$$@.tmp" || mv "$$@.tmp" "$$@"

ifeq (,$$(findstring $$(image_id),$$(targets_created)))
targets_created += $$(image_id)
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

