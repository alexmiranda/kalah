{ sources ? import ./nix/sources.nix { } }:
let pkgs = import sources.nixpkgs { };
in pkgs.mkShell rec {
  buildInputs = with pkgs; [
    graphviz
    jdk17_headless
    nixfmt
  ];
}
