# carmine-mq-issue-repro

`main` branch uses carmine v3.4.1

`old` branch uses carmine v3.2.0

- Run `lein run` in both branches and observe difference in time taken.
- Increasing nthreads worsens time in `main`.
- Using `:pool :none` in `main` allows for better performance than `old`.
