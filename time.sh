for X in {0,1,2,3,4,5,6,7,8,9}; do
  /usr/bin/time -f "mem=%D/%M, time=%E (%U/%S)" -- \
    java -ea -Xmx4096m -cp target/classes/:lib/com.microsoft.z3.jar synth.Main test-data/{01,02,03}*.txt > /dev/null
done

