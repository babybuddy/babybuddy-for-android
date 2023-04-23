.PHONY: all zxing-cpp

all: zxing-cpp

zxing-cpp: libs/zxingcpp-release.aar

zxing-cpp/wrappers/android/zxingcpp/build/outputs/aar/zxingcpp-release.aar:
	cd zxing-cpp/wrappers/android && ./gradlew :zxingcpp:assembleRelease

libs/zxingcpp-release.aar: zxing-cpp/wrappers/android/zxingcpp/build/outputs/aar/zxingcpp-release.aar
	cp $< $@
