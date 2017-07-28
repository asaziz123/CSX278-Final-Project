(ns engn-web.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [engn-web.core-test]))

(doo-tests 'engn-web.core-test)
