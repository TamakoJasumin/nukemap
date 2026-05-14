[Setup]
AppName=MIRV Sim
AppVersion=1.0.0
AppPublisher=MIRV Sim
DefaultDirName={autopf}\MIRV-Sim
DefaultGroupName=MIRV Sim
UninstallDisplayIcon={app}\MIRV-Sim.exe
Compression=lzma2
SolidCompression=yes
OutputDir=dist
OutputBaseFilename=MIRV-Sim-Setup-1.0.0
WizardStyle=modern
PrivilegesRequired=admin

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; GroupDescription: "Additional shortcuts:"

[Files]
Source: "dist\MIRV-Sim-win32-x64\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\MIRV Sim"; Filename: "{app}\MIRV-Sim.exe"
Name: "{commondesktop}\MIRV Sim"; Filename: "{app}\MIRV-Sim.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\MIRV-Sim.exe"; Description: "Launch MIRV Sim"; Flags: postinstall nowait skipifsilent
