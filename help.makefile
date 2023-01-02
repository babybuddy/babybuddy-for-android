.PHONY: all info refresh-flaticon-token

define convert_command =
	convert $(1) -crop "+0+170" -crop "-0-135" $(2)
endef

help_image_list := \
	help_insecure_http.png \
	help_insecure_http_warning.png \
	help_normal_login.png \
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

TARGET_PATH := app/src/main/res/drawable
SOURCE_PATH := resources/help_images

all: $(foreach x,$(help_image_list),$(TARGET_PATH)/$(x))
	
$(TARGET_PATH)/%.png: $(SOURCE_PATH)/screenshots/%.png
	$(call convert_command,$<,$@)

$(TARGET_PATH)/%.png: $(SOURCE_PATH)/direct/%.png
	cp $< $@
