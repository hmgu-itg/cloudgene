#!/usr/bin/bash

mvn install -Dmaven.test.skip=true

chmod +x ./target/cloudgene-2.8.7-assembly/cloudgene

rm -rvf ~/cloudgene_exec/webapp/
cp -rv ./target/cloudgene-2.8.7-assembly/webapp/ ~/cloudgene_exec/

rm -rvf ~/cloudgene_exec/sample
cp -rv ./target/cloudgene-2.8.7-assembly/sample ~/cloudgene_exec/

rm -rvf ~/cloudgene_exec/lib
cp -rv ./target/cloudgene-2.8.7-assembly/lib ~/cloudgene_exec/

rm -v ~/cloudgene_exec/cloudgene
cp -v ./target/cloudgene-2.8.7-assembly/cloudgene ~/cloudgene_exec/

rm -v ~/cloudgene_exec/cloudgene.jar
cp -v ./target/cloudgene-2.8.7-assembly/cloudgene.jar ~/cloudgene_exec/

exit 0
