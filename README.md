# traffic-control

## 대규모 트래픽 제어 실험실 - 우테코 오픈미션

## 실행환경

* java 21
* springboot 3.5.7
* H2 database
* prometheus
* grafana
* k6



## 1. JVMGC 수정하기



* 초기값 1GB로 시작.
* k6로 부하테스트 - 200명이 순차적으로 같은 api를 사용하고,  1분간 연속된 요청 보냄



* JVMGC를 2GB로 교체
* 같은 방식으로 부하 테스트 진행



=> 결과적으로 JVM을 수정하였음에도 오히려 멈춤시간이나 사용량이 늘어날뿐 상황이 개선되지 않음



---



* 추가적으로 다음 옵션을 추가함.
* \- XX:InitiatingHeapOccupancyPercent=30 (GC가 힙이 30퍼만 차도 시작되도록 함)
* \- Xlog:gc\*:file=gc\_log (gc log파일을 저장함)
* \- XX:+UseShenandoahGC (Shenandoah GC 활성화 옵션)



이유 : CPU를 좀 더 많이 쓰더라도 대용량 트래픽을 지연없이 처리하기 위해서

XX:InitiatingHeapOccupancyPercent=30으로 GC가 더 빨리 실행되도록하여 멈춤 시간을 줄임

XX:+UseShenandoahGC로 throughput(CPU 사용량)을 감수하고 지연 시간을 챙기는 GC옵션이라고 조사됨



=> 위 두가지 튜닝 옵션으로 초저지연 환경을 만들어 더 빠르게 안정화된 상태로 돌아오도록 하고싶었음

---

## 2. 코드 최적화 수행하기

* 현재 코드는 AI가 짜준 문제점이 있는 코드임
* 이를 AI를 사용하지 않고 최적화하는 것이 목표

### 2-1 N+1문제
- 기존 코드는 userRepository.findById(i)에서 n+1개의 쿼리를 날림

=> 해결을 위해서 한번의 쿼리로 모든 user를 가져오는 방식을 채택함.

- 문제 : count값이 long인데 List는 int범위의 데이터까지만 가져옴

### 2-2 gc최적화 문제
- 기존 코드는 log를 계속 String 객체로 만들어서 리스트로 저장함

=> 이를 해결하기 위해 List가 아닌 StringBuilder를 사용해서 로그를 합치기로 함

