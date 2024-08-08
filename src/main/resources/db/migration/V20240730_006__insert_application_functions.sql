/**
* OPAL Program
*
* MODULE      : insert_application_functions.sql
*
* DESCRIPTION : Inserts rows of data into the APPLICATION FUNCTIONS table.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ---------------------------------------------------------------------------------------------------------
* 30/07/2024    A Dennis    1.0         PO-537 Inserts rows of data into the APPLICATION FUNCTIONS table.
*
**/
INSERT INTO application_functions
(               
 application_function_id                 
,function_name                                       
)
VALUES
(
 35
,'Manual Account Creation'
);

INSERT INTO application_functions
(               
 application_function_id                 
,function_name                                       
)
VALUES
(
 41
,'Account Enquiry - Account Notes'
);

INSERT INTO application_functions
(               
 application_function_id                 
,function_name                                       
)
VALUES
(
 54
,'Account Enquiry'
);

INSERT INTO application_functions
(               
 application_function_id                 
,function_name                                       
)
VALUES
(
 500
,'Collection Order'
);
