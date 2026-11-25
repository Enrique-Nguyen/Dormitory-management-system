$ErrorActionPreference = 'Stop'
$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=Asia/Ho_Chi_Minh'
$env:PATH = 'C:\Users\Admin\.jdks\ms-17.0.17\bin;' + $env:PATH
$env:DMS_KEYCLOAK_CREDENTIAL_FILE = '.\data\local-run\keycloak-sample-accounts-1104b.local'
& 'D:\Dormitory-management-system\DMS\scripts\start-local.ps1' -ProvisionAccounts
