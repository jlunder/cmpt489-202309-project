/usr/bin/time -f "mem=%D/%M, time=%E (%U/%S)" -- \
  java -Xmx4096m -cp target/classes/:lib/com.microsoft.z3.jar synth.Main test-data/*.txt | tee results.txt

