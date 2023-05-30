#!/usr/bin/env bash
echo_info() {
  printf "[ \033[1;34m..\033[0m ] $1\n"
}
export $(cat ./variables.env | xargs)

if [ $# -eq 0 ]; then
  echo_error "No arguments supplied!\n"

  echo_info "Supported arguments:"

  echo_info "-----"
  echo_info "start - To Start the application"

  echo_info "-----"
  echo_info "init - Run this to make initial DB Inserts"

  exit 1
fi

if [ $1 = "start" ]; then
  echo_info "Starting File API"

  export ENV_DEBUG_MODE=true

  mvn spring-boot:run
else
  echo_error "Unknown command"
fi
