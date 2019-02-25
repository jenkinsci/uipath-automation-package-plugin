# Used at build time to load relevent UiPath modules responsible for packing and calling orchestrator APIs
# It loads UiPath.Powershell and UiPath.Extensions while packaging to store it in resources
# version is picked up from config.properties
[CmdletBinding()]
param(
    [String]$saveLocation,
    [String]$powershellVersion,
    [String]$extensionsVersion,
    [String]$namePowershell,
    [String]$nameExtensions
)
$repoLocation = "https://www.myget.org/F/uipath-dev/api/v2"
$psRepo = Get-PSRepository |  Where { $_.SourceLocation -eq $repoLocation }
if (($psRepo | measure).Count -eq 0)
{
    Register-PSRepository -Name UiPath-Dev -SourceLocation $repoLocation
    $psRepo = Get-PSRepository |  Where { $_.SourceLocation -eq $repoLocation }
    if (($psRepo | measure).Count -eq 0)
    {
        throw "Couldn't find the required repo, please validate the parameters if it is correct to register"
    }
}
Write-Host $saveLocation
if (-Not(Test-Path $saveLocation))
{
New-Item $saveLocation -Type Directory
}
Save-Module $namePowershell -Path $saveLocation -Repository $psRepo.Name -RequiredVersion $powershellVersion
Save-Module $nameExtensions -Path $saveLocation -Repository $psRepo.Name -RequiredVersion $extensionsVersion