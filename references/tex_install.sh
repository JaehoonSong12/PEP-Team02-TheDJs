#!/usr/bin/env bash

# Set the script to exit immediately if any command fails.
set -e

# Check if pdflatex is available in the system
if ! command -v pdflatex >/dev/null 2>&1; then
  # Install MacTeX using Homebrew Cask if not found
  brew install --cask mactex
else
  # Display the existing version if already installed
  echo "MacTeX is already installed: $(pdflatex --version | head -n1)"
fi

# Define the standard MacTeX binary path export statement
TEX_PATH='export PATH="/Library/TeX/texbin:$PATH"'

# Append the path to the environment configuration file if it is not already present
if ! grep -Fxq "$TEX_PATH" "$RC_FILE"; then
  echo "$TEX_PATH" >> "$RC_FILE"
fi