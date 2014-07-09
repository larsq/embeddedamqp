# Embedded AMQP Connection factory

The intention for this project is to implement a embedded AMQP for simulating
basic operations in environment when a real Rabbit MQ instance is not an option.

The ambition is to provide a trust-worthy driver for sending/receiving messages
for testing interfaces that relies on AMQP (and which a real server is not available).
It means that many operations are not available and the embedded server not be compliant 
with the AMQ specification but will sufficiently support client api. 

This release supports:
* declaration of exchanges, bindings and queues
* publishing, subscribing and getting sent messages (more work/tests are needed though)

The usage scenario is let executing test cases that is dependent of AMQ interface 
but where a real message broker cannot be installed or run.

## Installation

Clone this repository. This is utterly dependent of another project (springparent). The
main project will only import spring dependencies which is not necessarily needed and 
could easily be manually replaced with direct imports (TODO)
