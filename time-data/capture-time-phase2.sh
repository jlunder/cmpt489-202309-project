PHASE=phase2
TEST_DATA_DIR=test-data
H1_FILES=($TEST_DATA_DIR/$PHASE/02* $TEST_DATA_DIR/phase2/{0,0,0,0,0,0,0,0,0,0}{1,1,1,1,1,1,1,1,1,1}*)
H2_FILES=($TEST_DATA_DIR/$PHASE/03{0,1,2,3,4}* $TEST_DATA_DIR/phase2/{02,02,02,02,02}*)
H3_FILES=($TEST_DATA_DIR/$PHASE/04* $TEST_DATA_DIR/phase2/03*)

for X in {0,1}{0,1,2,3,4,5,6,7,8,9}; do
  CAPFILE=time-data/$PHASE/perfcap-h1-$X.txt
  echo "Capturing $CAPFILE"
  /bin/time -o $CAPFILE -a java -ea -cp lib/com.microsoft.z3.jar:target/classes synth.Main \
    ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} \
    ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} ${H1_FILES[*]} \
    2>&1 > $CAPFILE

  CAPFILE=time-data/$PHASE/perfcap-h2-$X.txt
  echo "Capturing $CAPFILE"
  /bin/time -o $CAPFILE -a java -ea -cp lib/com.microsoft.z3.jar:target/classes synth.Main \
    ${H2_FILES[*]} 2>&1 > $CAPFILE

  CAPFILE=time-data/$PHASE/perfcap-h3-$X.txt
  echo "Capturing $CAPFILE"
  /bin/time -o $CAPFILE -a java -ea -cp lib/com.microsoft.z3.jar:target/classes synth.Main \
    ${H3_FILES[*]} 2>&1 > $CAPFILE
done

echo "Done."

