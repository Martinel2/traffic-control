# traffic-control

## 대규모 트래픽 제어 실험실 - 우테코 오픈미션

## 실행 환경

* java 21
* springboot 3.5.7
* H2 database
* prometheus
* grafana
* k6

## 실행 방법

docker-compose.yml이 있는 곳에서 터미널을 연다.

도커가 없다면 도커를 설치한다.

먼저, 이 프로젝트의 jar파일을 만든다.
```bash
./gradlew bootjar
```

그 후, 도커에 prometheus,grafana,spring-boot 서버를 올린다.
```bash
docker compose up -d --build
```

http://localhost:3000에 접속하여 grafana를 확인한다.

- 초기 아이디 비밀번호는 admin/admin이며, 수정가능하다.



## 1. JVMGC 수정하기

* 초기값 1GB로 시작.
* k6로 부하테스트 - 200명이 순차적으로 같은 api를 사용하고,  1분간 연속된 요청 보냄

### JAVA_OPTS=-Xms512m -Xmx1g (초기 실험 상태. 다른 옵션 x)
<img width="914" height="750" alt="1기가일때" src="https://github.com/user-attachments/assets/0a6c1d1f-c882-4126-8a0d-a69799e891d5" />
<img width="1376" height="907" alt="1기가 k6통계" src="https://github.com/user-attachments/assets/7391800c-da9f-4e9c-9377-979a72efeb40" />

### JAVA_OPTS=-Xms512m -Xmx2g (최대 힙 크기만 변경. 다른 옵션 x)
* JVMGC를 2GB로 교체
* 같은 방식으로 부하 테스트 진행
<img width="888" height="729" alt="2기가일때" src="https://github.com/user-attachments/assets/3b54a712-695a-46f7-babd-3926f1ffea72" />
<img width="1363" height="865" alt="2기가 k6 통계" src="https://github.com/user-attachments/assets/cbfcb88b-ec0f-44d2-a33e-82b1b7b8d694" />

=> 결과적으로 JVM을 수정하였음에도 오히려 멈춤시간이나 사용량이 늘어날뿐 상황이 개선되지 않음

---

* 추가적으로 다음 옵션을 추가함.
    * \- XX:InitiatingHeapOccupancyPercent=30 (GC가 힙이 30퍼만 차도 시작되도록 함)
    * \- Xlog:gc\*:file=gc\_log (gc log파일을 저장함)
    * \- XX:+UseShenandoahGC (Shenandoah GC 활성화 옵션)



이유 : CPU를 좀 더 많이 쓰더라도 대용량 트래픽을 지연없이 처리하기 위해서
- XX:InitiatingHeapOccupancyPercent=30으로 GC가 더 빨리 실행되도록하여 멈춤 시간을 줄임
- XX:+UseShenandoahGC로 throughput(CPU 사용량)을 감수하고 지연 시간을 챙기는 GC옵션이라고 조사됨

=> 위 두가지 튜닝 옵션으로 초저지연 환경을 만들어 더 빠르게 안정화된 상태로 돌아오도록 하고싶었음

### 실행 결과

### JAVA_OPTS=-Xms512m -Xmx1g(위에 제시한 모든 옵션 사용)
<img width="896" height="774" alt="1기가-추가옵션" src="https://github.com/user-attachments/assets/490c2454-c45f-4e4d-8c2b-3eb4946d159e" />
<img width="1378" height="873" alt="1기가-추가 옵션 추가 통계" src="https://github.com/user-attachments/assets/c1094d93-030e-4f46-8802-ad4dec0771d7" />

### JAVA_OPTS=-Xms1g -Xmx2g(위에 제시한 모든 옵션 사용)
<img width="900" height="726" alt="초기1최대2기가" src="https://github.com/user-attachments/assets/d5d910dc-cbb4-46ba-99f1-ea1f6c176fe6" />
<img width="1382" height="855" alt="초기1최대2기가 k6통계" src="https://github.com/user-attachments/assets/f685e4e0-277d-45b7-939a-d574a39eb37a" />

### JAVA_OPTS=-Xms512m -Xmx2g(위에 제시한 모든 옵션 사용)
<img width="904" height="721" alt="초0 5최대2" src="https://github.com/user-attachments/assets/61749161-7e95-4ab4-9a1b-bea860d60c01" />
<img width="1380" height="868" alt="초0 5최대2통계" src="https://github.com/user-attachments/assets/e4a12df0-4595-4af4-8eb7-65c70c7c1a91" />

### 결과 요약
- 힙 크기만 변경하였을 때는 큰 효과를 보지 못하였음.
- 다른 옵션들을 모두 사용하니 같은 힙 크기 기준 아주 좋은 성능개선을 보임.

- 최대 힙 크기 1g 기준, http_req_duration에서,
- 최대 지연 시간이 6.29s -> 2.79s
- p(95) (사용자중 5명) 은 1.22s -> 226.31ms
- vus(최대 가능 동시접속자 수)는 1->12

- 그래프에서도 성능 개선 확인 가능
- JVM GC 멈춤 시간은 다소 증가함.
- JVM 사용량도 최대치는 비슷하나, 상대적으로 빠르게 안정화되는 모습을 보임(15초차이)
- 특히, 초기값 0.5g - 최대값 2g일때 가장 좋은 성능을 보임.

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

### 실행 결과

<img width="905" height="724" alt="스크린샷 2025-11-15 181455" src="https://github.com/user-attachments/assets/5c1b18b6-a0d1-494c-9eca-8290022029ce" />
<img width="1359" height="864" alt="스크린샷 2025-11-15 181508" src="https://github.com/user-attachments/assets/656ffa51-e593-44e5-8d2a-fa7ac5bcd8cd" />

- 코드를 최적화했다고 생각하고 실행을 해보니 오히려 성능이 저하됨.
- JVM 사용량, GC 멈춤 시간 둘다 엄청나게 늘어남.
- 사용량은 1.의 최적조합에서 최대 40,000,000이였던 반면, 2.에서는 150,000,000을 넘는 사용량을 보임.
- 멈춤시간 또한 1.의 최적조합에서 최대 0.015초 미만에 지속시간 약 1분 15초였던 반면,
  멈춤시간 약 0.0360초에 지속시간 3분으로 성능 저하가 발생함.

=> n+1문제를 한 번에 모두 가져오는 방식으로 처리하면 오히려 객체가 증가하여 성능저하가 발생.
=> 또한, GC 최적화 문제도 결국 로그가 쌓여서 메모리를 차지하게되므로, 멈춤을 조장하는 사태가 발생.

두가지 경우 모두 일정크기로 값을 나눠서 받아 처리하는 분할 처리 시스템으로 재구성해야할 것 같다고 생각함.

---

## 3. 코드 재 수정 - 분할하여 처리하는 시스템으로 재구성

- 기존에는 count만큼 모든 객체를 가져와서 메모리상 무리가 오는 경우가 존재.
- 따라서, 페이지네이션기법을 통해 user를 나누어 받도록 개선.
- 로그 또한 StringBuilder가 아닌 Logger를 사용하여 분할 처리하도록 개선

### 실행결과

<img width="896" height="720" alt="코드 변경" src="https://github.com/user-attachments/assets/725545cf-f375-4058-9aca-fff5e66ed7cd" />
<img width="1379" height="887" alt="코드 변경 통계" src="https://github.com/user-attachments/assets/2a5a1afd-b7eb-4037-9cb9-3ec39a77712b" />

- 통계상으로는 JVM 튜닝 최적 옵션일 때와 거의 유사함.
- JVM 사용량과 멈춤 시간 부분에서 유의미한 성능개선이 포착됨.
- 사용량이 기존에는 일자 형태로 지속되다가 테스트시에 요동쳤음.
- 현재는 일정한 주기로 톱니 패턴이 반복되는 형태를 보임.
- 톱니 패턴 진행중 테스트 시, 테스트가 끝난 후엔 바로 원래 주기로 돌아가는 모습을 보임.

### 톱니 패턴은 왜 생겼나?
- 요청을 주지 않았음에도 톱니패턴이 생겨 문제가 생겼다고 생각하여 조사함.
- 알아보니, Young/Old Gen 영역 관련 내용을 내가 모르고 있어서 착각했던 것.
- 기존의 코드는 Old Gen 영역을 주로 사용하여 GC가 테스트 시에만 가동함.
- 그래서 튜닝 옵션으로 아주 큰 성능 개선을 볼 수 있었던 것.
- 최종 코드는 Young Gen 영역에 객체를 생성하고 주기를 다하면 바로 GC로 삭제함.
- 따라서, 일정한 주기마다 심장박동과 같이 객체를 생성,삭제하며 서버를 유지하는 모습이였던 것.

=> 그러면 여기서 튜닝 옵션을 빼고 돌리면 어떻게 될까?

### 추가 실험 결과

- 옵션을 모두 제거한 경우
    - k6통계가 가장 좋았음.
    - 다만, 그래프 상으로 멈춤시간이 0.01초 늘어남.
    - 또한, 사용량도 튀는 현상이 발생함.
<img width="899" height="755" alt="최종코드, 옵션x 그래프" src="https://github.com/user-attachments/assets/4594eceb-07b0-48a0-9a70-26edcc8003da" />
<img width="1388" height="887" alt="최종코드, 옵션x" src="https://github.com/user-attachments/assets/15be4060-785d-4160-92ec-6ebcb756bfb2" />

- Shenandoah GC만 사용한 경우
    - 애매한 결과가 나옴.
    - 그래프상으로도, k6 통계도 지금까지 통계의 평균치인 것 같음.
    - 강점이 크게 부각되지 않는 느낌이 들었음.
<img width="907" height="752" alt="최종코드,shen" src="https://github.com/user-attachments/assets/250e427f-c191-41f2-a0f6-02eeaa8ba09d" />
<img width="1386" height="894" alt="최종코드, shen, 통계" src="https://github.com/user-attachments/assets/6e55b292-0026-48bf-aee0-1ad28951127c" />

---

## 결론

- '대용량' 트래픽을 제어한다면 디폴트 옵션이 생각보다 좋음.
- '동시 접속의 대용량' 트래픽을 제어한다면 JVM GC를 튜닝해보는 것이 좋음.
- 코드를 작성할 때, 메모리를 생각하며, 효율적으로 작동하도록 코드를 구성하기.
- 하드웨어가 좋아졌다지만, 최적화는 항상 옳다.
- 아주 짧은 시간동안 진행된 프로젝트여서 아직 모르는 옵션이나 정보가 많을 것 같음. 더 깊게 공부를 해봐야 할 듯하다.
