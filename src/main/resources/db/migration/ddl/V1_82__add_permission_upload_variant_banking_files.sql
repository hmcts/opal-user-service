/**
* OPAL Program
*
* MODULE      : add_permission_upload_variant_banking_files.sql
*
* DESCRIPTION : Add the Upload Variant Banking Files permission.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 18/11/2026    T McCallion    1.0         PO-8684 - FE - Upload Variant Banking Files. 
*                                          Part of EI4: Interface File Viewer & Variant Banking
*
**/

ALTER TYPE t_permissions_enum
    ADD VALUE IF NOT EXISTS 'UPLOAD_VARIANT_BANKING_FILES';