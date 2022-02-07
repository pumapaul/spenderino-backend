# spenderino-backend

This repository contains the source code necessary to run a backend that can serve the apps defined in the [Spenderino](https://github.com/pumapaul/spenderino) project.

Due to the nature of my bachelor's thesis being focused on the apps themselves, this backend implementation is more of a stub. 
It's implemented in Ktor and there is no database or anything, everything is being done in memory. 

The backend is currently hosted on [heroku](heroku.com) with a free subscription tier. That means, whenever the app sleeps (after 30 minutes of inactivity), the memory is reset. 

You can always host it yourself if you want to play around with it.



## Getting Started

* Clone the repository
* Configure the JWT signing. We're using RS256 as a signing algorithm.
  * You need to provide a JWK specification in `/certs/jwks.json`
  * You need to provide a private key for this JWK spec by either
    * providing an environment variable named `JWT_PRIVATE_KEY` or
    * providing a `jwt.privateKey` inside `/src/main/resources/application.conf`

* If you want everything related to payment via [Stripe](https://stripe.com) to work, you'll need to provide the respective API secrets either as environment variables or inside `/src/main/resources/application.conf`
  * general API key: `STRIPE_API_KEY` or `stripe.apiKey`
  * webhook secret: `STRIPE_WEBHOOK_SECRET` or `stripe.webhookSecret`
* Build the app via gradle
  `$ ./gradlew installDist`
* Run the app
  `$ ./build/install/spenderino-backend/bin/spenderino-backend`



If you need to change the host or port the app is running on, you can do that in the `application.conf` file. If you need help, you can also refer to the Ktor documentation: https://ktor.io/docs/configurations.html#hocon-file



## Dependencies

* [Ktor](https://ktor.io) as a framework
* [ZXing](https://github.com/zxing/zxing) for creating QR codes
* [Stripe](https://stripe.com) as a payment provider
* [Bcrypt Java Library](https://github.com/patrickfav/bcrypt) for hashing passwords