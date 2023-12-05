H1_FILES=(../../Synth/test-data/phase1/02* ../../Synth/test-data/phase2/{0,0,0,0,0,0,0,0,0,0}{1,1,1,1,1,1,1,1,1,1}*)
H2_FILES=(../../Synth/test-data/phase1/03{0,1,2,3,4}* ../../Synth/test-data/phase2/{02,02,02,02,02}*)
H3_FILES=(../../Synth/test-data/phase1/04* ../../Synth/test-data/phase2/03*)

for X in {0,1}{0,1,2,3,4,5,6,7,8,9}; do
  CAPFILE=time-data/phase1/perfcap-h1-$X.txt
  echo "Capturing $CAPFILE"
  /bin/time -o $CAPFILE -a java -ea -cp lib/com.microsoft.z3.jar:target/classes synth.Main \
    ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} \
    ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} \
    2>&1 > $CAPFILE
  CAPFILE=time-data/phase1/perfcap-h2-$X.txt
  echo "Capturing $CAPFILE"
  /bin/time -o $CAPFILE -a java -ea -cp lib/com.microsoft.z3.jar:target/classes synth.Main \
    ${H2_FILES[*]} 2>&1 > $CAPFILE
  CAPFILE=time-data/phase1/perfcap-h3-$X.txt
done

echo "Capturing $CAPFILE"
/bin/time -o $CAPFILE -a java -ea -cp lib/com.microsoft.z3.jar:target/classes synth.Main \
  ${H3_FILES[*]} 2>&1 > $CAPFILE

echo "Done."

