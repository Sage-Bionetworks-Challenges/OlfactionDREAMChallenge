~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S1_validation.pl 

USAGE:perl DREAM_Olfactory_S1_validation.pl  <input file to validate> <output file> <flag L for Leaderboard or F for final submission>  

Format validation for DREAM9.5 Olfactory Prediction Sub Challenge 1
example: perl DREAM_Olfactory_S1_validation.pl  my_prediction.txt errors.txt L


~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S1_validation.pl test_LB_s1_ok.txt errors_ok.txt X
phase has to be either L or F not X. Bye...


~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S1_validation.pl test_LB_s1_ok.txt errors_ok.txt L
reading test_LB_s1_ok.txt


~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > cat errors_ok.txt 
OK
Valid data file

~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S1_validation.pl test_LB_s1_short.txt  errors_short.txt L
reading test_LB_s1_short.txt
~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > cat errors_short.txt 
NOT_OK
You have the following error(s): Missing predictions for 1030 1 INTENSITY/STRENGTH entry
Missing predictions for 1060 1 INTENSITY/STRENGTH entry
Missing predictions for 2682 1 INTENSITY/STRENGTH entry
Missing predictions for 6137 1 INTENSITY/STRENGTH entry

Please see the template file and  resubmit the updated file.

~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S1_validation.pl test_LB_s1_repeats.txt errors_repeats.txt L
reading test_LB_s1_repeats.txt
~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > cat errors_repeats.txt 
NOT_OK
You have the following error(s): 2682, 1 with INTENSITY/STRENGTH is a  duplicated entry. Error at input line # 8.

Please see the template file and resubmit the updated file.

~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S1_validation.pl test_LB_s1_low_val.txt  errors_low_val.txt L
reading test_LB_s1_low_val.txt
~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > cat errors_low_val.txt 
NOT_OK
You have the following error(s): Value must be a positive float number, less or equal to 100. Got -28, which is incorrect.
Error at input line # 5.

Please see the template file and resubmit the updated file.

###MOVED INPUT AND OUTPUT FILES TO DIRECTORY Test_LB_S1
~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S2_validation.pl test_LB_s2_ok.txt errors_LB_s2_ok.txt L
reading test_LB_s2_ok.txt
~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > cat errors_LB_s2_ok.txt 
OK
Valid data file

~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S2_validation.pl test_LB_s2_ok.txt errors_LB_s2_ok.txt S
phase has to be either L or F not S. Bye...

~/Documents/Projects/DREAMs/DREAMs/DREAM9/DREAM9.5_Olfactory/Validation > perl DREAM_Olfactory_S2_validation.pl test_LB_s2_ok.txt  S

USAGE:perl DREAM_Olfactory_S2_validation.pl  <input file to validate> <output file> <flag L for Leaderboard or F for final submission>  

Format validation for DREAM9.5 Olfactory Prediction Sub Challenge 2
example: perl DREAM_Olfactory_S2_validation.pl  my_prediction.txt errors.txt L