.PHONY: all info refresh-flaticon-token stringsxml

define convert_command =
	convert $(1) -crop "+0+170" -crop "-0-135" $(2)
endef

help_image_list := \
	help_insecure_http.png \
	help_insecure_http_warning.png \
	help_timer_setup.png \
	help_create_default_timers.png \
	help_create_default_timers_menu.png \
	help_timers_configure.png \
	help_overview.png \
	help_events.png \
	help_feeding_form.png \
	help_play_button_highlight.png \
	help_notes_button_highlight.png \
	help_save_button_highlight.png \
	help_save_diaper_highlight.png \
	help_timers_configure2.png \
	help_login_page.png \
	help_login_qrcode.png \
	help_login_homeassistant.png \

TARGET_PATH := app/src/main/res/drawable
SOURCE_PATH := resources/help_images

all: $(foreach x,$(help_image_list),$(TARGET_PATH)/$(x)) stringsxml

HELP_POSTFIXES := \
	-nl \

stringsxml: $(foreach x,$(HELP_POSTFIXES),app/src/main/res/values$(x)/help_strings.xml)

app/src/main/res/values/help_strings.xml: $(SOURCE_PATH)/help.md $(SOURCE_PATH)/process_markdown.py
	cd $(SOURCE_PATH) \
	&& python3 -m pipenv install -r requirements.txt \
	&& python3 -m pipenv run python process_markdown.py $(abspath $<) $(abspath $@)

define _target_gen =

app/src/main/res/values$(1)/help_strings.xml: $$(SOURCE_PATH)/help$(1).md $$(SOURCE_PATH)/process_markdown.py
	cd $$(SOURCE_PATH) \
	&& python3 -m pipenv install -r requirements.txt \
	&& python3 -m pipenv run python process_markdown.py $(abspath $$<) $(abspath $$@)

endef

$(foreach x,$(HELP_POSTFIXES),$(eval $(call _target_gen,$(x))))

app/src/main/res/values/help_strings.xml: $(SOURCE_PATH)/help.md $(SOURCE_PATH)/process_markdown.py
	cd $(SOURCE_PATH) \
	&& python3 -m pipenv install -r requirements.txt \
	&& python3 -m pipenv run python process_markdown.py $(abspath $<) $(abspath $@)

$(TARGET_PATH)/%.png: $(SOURCE_PATH)/screenshots/%.png
	$(call convert_command,$<,$@)

$(TARGET_PATH)/%.png: $(SOURCE_PATH)/direct/%.png
	cp $< $@
