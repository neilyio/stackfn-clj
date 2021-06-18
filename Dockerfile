FROM clojure


WORKDIR /root/rpl-interpreter

COPY deps.edn ./deps.edn

RUN clojure -P -Sverbose

COPY . .

ENTRYPOINT ["clojure"]
