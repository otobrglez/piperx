{ pkgs, lib, config, inputs, ... }:

{
  name = "piperx";

  languages.java.jdk.package = pkgs.jdk21_headless;
  languages.scala = {
    enable = true;
    sbt.enable = true;
  };

  packages = [ 
  ];

  enterShell = ''
  '';
  
  enterTest = ''
  	sbt test
  '';
}
