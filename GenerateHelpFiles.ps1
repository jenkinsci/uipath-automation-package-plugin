$OpenDivTag='<div>'
$CloseDivTag='</div>'
$OpenHrefTag='<a href ="'
$CloseHrefTag='" target = "_blank">here</a>.'

$UrlRegex = "https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)"

$ResourcesRelativePath = "src\\main\\resources\\com\\uipath\\uipathpackage\\"
$FolderName = "help"
$ActivitiesFolders = @{"TokenAuthenticationEntry" = "entries\\authentication\\TokenAuthenticationEntry";
                        "DynamicallyEntry" = "entries\\job\\DynamicallyEntry";
                        "RobotEntry" = "entries\\job\RobotEntry";
                        "TestProjectEntry" = "entries\\testExecutionTarget\\TestProjectEntry";
                        "TestSetEntry" = "entries\\testExecutionTarget\\TestSetEntry";
                        "UiPathAssets" = "UiPathAssets";
                        "UiPathDeploy" = "UiPathDeploy";
                        "UiPathPack" = "UiPathPack";
                        "UiPathRunJob" = "UiPathRunJob";
                        "UiPathTest" = "UiPathTest"}

$Languages = @("", "_de_DE", "_es", "_es_MX", "_fr", "_ja", "_ko", "_pt", "_pt_BR", "_ru", "_tr", "_zh_CN")

# Set content of a file using FilePath and Value
function CreateFileWithContent {
    param (
        $FilePath,
        $Value
    )

    $hasUrl = $Value -match $UrlRegex
    if ($hasUrl -eq $true){
        $match = $matches[0]
        $Value = $Value.Replace($match, "$OpenHrefTag$match$CloseHrefTag")
    }
    $TemplateValue = $OpenDivTag + $Value + $CloseDivTag
    Set-Content -Path $FilePath -Value $TemplateValue -Force
}

# Create the html help files for jelly interfaces
function CreateHelpFiles {
    # Iterate through help.properties files to create help.html files for each language
    foreach ($language in $Languages) {
        $content = Get-Content $ResourcesRelativePath\help$language.properties -Force

        foreach ($line in $content) {
            # Get the key-value pairs and create a html file for each entry in the properties file
            $model, $key = $line.Split('=')[0].Split('.')
            $value = $line.Split('=')[1]

            $subfolder = $ActivitiesFolders[$model]

            $FileName = "$FolderName-$key$language.html"
            $FilePath = ".\$ResourcesRelativePath\$subfolder\$Filename"

            CreateFileWithContent -FilePath $FilePath -Value $value
        }
    }
}

CreateHelpFiles