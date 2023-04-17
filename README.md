## io-utils
Set of IO utility classes

#### LineSuffixBufferedReader 
Read character stream while appending a suffix at each line break. 

    LineSuffixBufferedReader r = new LineSuffixBufferedReader(
                                        new FileReader("/tmp/test.csv"),
                                        ",customer\n", 
                                        ",record_type\n"
                                    );
