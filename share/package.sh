#!/usr/bin/env sh

if [ $# -eq 0 ]
  then
    echo "Path to package need to be specify"
	exit 1
fi

if [[ `mkdir -p "work/gradle/wrapper"` -ne 0 ]];
	then
        echo "Cannot create work folder"
        exit 1
fi

baseUrl=https://raw.githubusercontent.com/Cognifide/gradle-aem-plugin/master/share/wrapper
declare -a arr=("gradlew" "build.gradle" "settings.gradle" "gradle/wrapper/gradle-wrapper.properties" "gradle/wrapper/gradle-wrapper.jar")

for i in "${arr[@]}"
do
	response=$(curl "$baseUrl/$i" --write-out %{http_code} --silent --output "work/$i")  
	if [ $response -ne 200 ];
	then
        echo "Cannot download wrapper"
		rm -rf work
        exit 1
fi
done

cd work
sh gradlew aemSatisfy -Paem.satisfy.urls=[$1]
cd .. && rm -rf work