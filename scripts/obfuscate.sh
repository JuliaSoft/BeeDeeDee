# This script generates the obfuscates code for the
# BeeDeeDee library and runs the original code first
# and the obfuscated code later

cd bin
time java -Xmx1000m com.juliasoft.beedeedee.examples.MultiQueens 10 9 8
jar -cf beedeedee.jar com/*
java -Xmx1000m -classpath ../../julia-annotations/bin:../../julia-core/bin:../../julia-core/lib/bcel-5.2.jar:../../julia-core/lib/android.jar:../../julia-core/lib/javabdd-1.0b2.jar com.juliasoft.julia.engine.Julia -si beedeedee.jar -i java.lang. -jb -outputPath obfuscated -framework java -Obfuscator where expAlias kind loopOnCalculations
cd obfuscated
time java -Xmx1000m com.juliasoft.beedeedee.examples.MultiQueens 10 9 8
cd ..
#rm -r obfuscated
rm beedeedee.jar
cd ..

