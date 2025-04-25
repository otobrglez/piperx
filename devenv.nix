{ pkgs, lib, config, inputs, ... }:

{
  name = "piperx";

  languages.java.jdk.package = pkgs.jdk21_headless;
  languages.scala = {
    enable = true;
    sbt.enable = true;
  };

  packages = [
    pkgs.awscli2
  ];

  enterShell = ''
      export AWS_ACCESS_KEY_ID="test"
      export AWS_SECRET_ACCESS_KEY="test"
      export AWS_DEFAULT_REGION="us-east-1"
  '';
  
  enterTest = ''
  	sbt test
  '';
}
