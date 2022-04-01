SPACE := $(null) $(null)

FREE_IMAGES = \
	feeding-breast.png==pkg_breast

FLATICON_IMAGES = \
	4063767?size=512==pkg_poop

VARIANTS = \
	drawable-mdpi==24 \
	drawable-hdpi==36 \
	drawable-xhdpi==48 \
	drawable-xxhdpi==72 \
	drawable-xxxhdpi==96 \

VARIANTS_BASE_PATH := app/src/main/res/

get_field = $(word $(1),$(subst ==,$(SPACE),$(2)))

ALL_TARGETS = $(foreach x,$(FLATICON_IMAGES),$(foreach v,$(VARIANTS), $(VARIANTS_BASE_PATH)$(call get_field,1,$(v))/$(call get_field,2,$(x)).png))

.PHONY: all refresh-flaticon-token

all: $(ALL_TARGETS)

dtest:


flaticon-apikey:
	[ -e flaticon-apikey ] || ( echo "You need to create a flaticon api key first" && exit 1 )

refresh-flaticon-token: flaticon-apikey
	@bash utils/test_flaticontoken.sh ./flaticon-token && \
	echo "Flaticon token still valid" || \
	( \
		curl -s -X POST -H "Accept: application/json" --data apikey=$$( cat ./flaticon-apikey ) https://api.flaticon.com/v3/app/authentication | jq -r ".data.token" > ./flaticon-token && \
		echo Flaticon token refreshed \
	)


# Generate variant pngs
define _variants_macro =

$(VARIANTS_BASE_PATH)$$(call get_field,1,$(1))/%.png: resources/nonfree/%.png
	@echo "DO STH $$@  $$^ "$$(call get_field,2,$(1))
	convert "$$^" -resize "$$(call get_field,2,$(1))x$$(call get_field,2,$(1))" "$$@"

endef

$(foreach v,$(VARIANTS),$(eval $(call _variants_macro,$(v))))

# Download images
define _flaticon_image =

resources/nonfree/$$(call get_field,2,$(1)).png: refresh-flaticon-token
	[ -e "$$@" ] || \
	curl -X GET \
		-H "Accept: application/json" \
		-H "Authorization: Bearer $$$$( cat ./flaticon-token )" \
		--output $$@ \
		"https://api.flaticon.com/v2/item/icon/download/$$(call get_field,1,$(1))"
	
endef

$(foreach flaticon,$(FLATICON_IMAGES), $(eval $(call _flaticon_image,$(flaticon))))

