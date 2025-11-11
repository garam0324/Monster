import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

// 몬스터 행동 인터페이스
interface MonsterAction {
    void performAction();
    void die();
}

// 몬스터 클래스 + 이동 스레드
class Monster {
    private JLabel label;
    private Thread moveThread;
    private boolean isAlive;

    // 몬스터 생성자
    public Monster(String type, Color color) {
        label = new JLabel(type); // 라벨 이름 : 공격 or 방어
        label.setHorizontalAlignment(SwingConstants.CENTER); // 글자 가운데 정렬
        label.setForeground(Color.WHITE); // 글자 색 : 흰색
        label.setBackground(color); // 라벨 색
        label.setOpaque(true); // 투명하게
        label.setSize(50, 50); // 크기 50 * 50
        label.setLocation(getRandomX(), getRandomY()); // 랜덤 위치
        moveThread = new Thread(() -> move()); // 몬스터 이동을 위한 스레드 생성(Runnable 이면 스레드 실행)
        moveThread.start(); // 스레드 시작
        isAlive = true;
    }

    // 랜덤 X 좌표
    private int getRandomX() {
        return (int) (Math.random() * (500 - 80));
    }

    // 랜덤 Y 좌표
    private int getRandomY() {
        return (int) (Math.random() * (500 - 80));
    }

    // JLable 반환
    public JLabel getLabel() {
        return label;
    }

    // 몬스터 이동 메서드
    private void move() {
        while (true) {
            try {
                Thread.sleep(1000); // 1초 간격으로 이동
            }
            catch (InterruptedException e) {
                return;
            }

            label.setLocation(getRandomX(), getRandomY()); // 랜덤 위치 설정
        }
    }
    
    // 몬스터가 살아 있는지 여부를 반환하는 메서드
    public boolean isAlive() {
        return isAlive;
    }

    // 몬스터가 죽었을 때 호출되는 메서드
    public void die() {
        isAlive = false;
    }
}

//공격 몬스터 클래스
class AttackMonster extends Monster implements MonsterAction {
	private Player player;
	private static final int BASE_ATTACK_DAMAGE = 2; // 기본 데미지 : 2
	private int attackDamage; // 공격 데미지
	private boolean attacking; // 몬스터가 공격 중인지 여부
	private static int aliveMonsterCount = 10; // 처음 몬스터 수

	// AttackMonster 생성자
    public AttackMonster(Player player) {
        super("(╬ಠ益ಠ)", Color.RED);
        this.player = player;
        this.attackDamage = BASE_ATTACK_DAMAGE;
        attacking = true;

        // 공격 스레드 시작
        startAttackThread();
    }

	// 오버라이딩 - 공격 몬스터 액션
	public void performAction() {
		if (attacking) {
			System.out.println("공격!!!");
			player.decreaseHealth(attackDamage);
		}
	}

	// 공격 스레드를 시작하기 위한 메서드
    private void startAttackThread() {
        AttackThread attackThread = new AttackThread(AttackMonster.this);
        attackThread.start();
    }

    // 공격 몬스터가 죽을 때마다 공격 데미지가 감소하는 메서드
	private synchronized void reduceAttackDamage() {
		aliveMonsterCount--;
		attackDamage = BASE_ATTACK_DAMAGE * aliveMonsterCount;
	}

	// 몬스터가 죽었을 때 호출되는 메서드
	public void die() {
		super.die(); // 부모 클래스의 die 메서드 호출
		attacking = false; // 몬스터가 죽었으므로 공격 중지
		reduceAttackDamage(); // 몬스터가 죽었으므로 공격 데미지 재조정
	}
	
	// 공격 여부를 설정하는 메서드
	public void setAttacking(boolean b) {
		this.attacking = b;
	}
}
    
// AttackThread 클래스 (공격 몬스터가 공격을 수행하도록 함)
class AttackThread extends Thread {
    private AttackMonster attackMonster;

    public AttackThread(AttackMonster attackMonster) {
        this.attackMonster = attackMonster;
     }

    // 5초마다 플레이어를 공격함
    public void run() {
        try {
            while (attackMonster.isAlive()) {
                Thread.sleep(5000); // 5초 대기
                attackMonster.performAction();
            }
        }
        catch (InterruptedException e) {
            return;
        }
    }
}

// 방어 몬스터 클래스
class DefenseMonster extends Monster implements MonsterAction{
	
	// 방어 몬스터 생성자
    public DefenseMonster() {
        super("(ಠ_ಠ)", Color.BLUE);
    }

    // 오버라이딩
    public void performAction() {
        System.out.println("방어!!!");
    }

	// 오버라이딩
	public void die() {
		// 없음
	}
}

//플레이어 클래스
class Player {
	private int health;
	private JLabel healthLabel; // 추가된 부분

	// JLabel을 받지 않는 플레이어 클래스 생성자
	public Player() {
        health = 100; // 기본 체력 : 100
    }
	
	// JLabel을 받는 플레이어 클래스 생성자
	public Player(JLabel healthLabel) {
		health = 100;
		this.healthLabel = healthLabel;
        updateHealthLabel(); // 초기 체력 설정
	}

	// 체력 감소(공격 몬스터에게 데미지를 받을 경우)
	public void decreaseHealth(int damage) {
		health -= damage;
		if(health <= 0) { // 플레이어의 체력이 0 이하면 게임 오버
			gameOver();
		}
		updateHealthLabel(); // 플레이어 체력이 갱신되면 화면에 반영
	}
	
	// 체력 라벨 갱신
	private void updateHealthLabel() {
        healthLabel.setText("플레이어 체력: " + health);
    }

	public int getHealth() {
		return health;
	}
	
	// 게임 오버 메서드
	private void gameOver() {
        JOptionPane.showMessageDialog(null, "Game Over!");
        System.exit(0); // 게임 종료
    }
}

//게임
public class MonsterGame extends JFrame {
	private int count; // 몬스터 처치 수
	private Player player; // 플레이어 생성
	
	// 처치 횟수, 체력, 게임 방법을 보여주는 JLabel
	private JLabel countLabel;
	private JLabel healthLabel;
	private JLabel messageLabel1;
	private JLabel messageLabel2;
	private JLabel messageLabel3;

	// 전체 몬스터 수
	private static final int TOTAL_MONSTERS = 20;

	public MonsterGame() {
		count = 0; // 초기 처치 횟수 설정

		player = new Player(); // 플레이어 객체 생성
		
		// 처치 횟수 라벨 생성
		countLabel = new JLabel("몬스터 " + count + "마리 처치!!");
		countLabel.setBounds(10, 10, 170, 15);
		countLabel.setBackground(Color.YELLOW);
		countLabel.setOpaque(true);
		add(countLabel);

		// 체력 라벨 생성
		healthLabel = new JLabel("플레이어 체력 : " + player.getHealth());
		healthLabel.setBounds(10, 25, 170, 15);
		healthLabel.setBackground(Color.YELLOW);
		healthLabel.setOpaque(true);
		add(healthLabel);
		
		// 체력 라벨을 플레이어 클래스의 매개변수로 넣어 공격 몬스터에게 공격을 받을 때마다 라벨이 갱신되도록 해줌
		player = new Player(healthLabel);

		// 게임 방법 라벨 생성
		messageLabel1 = new JLabel("<게임 방법>");
		messageLabel1.setBounds(10, 40, 170, 15);
		messageLabel1.setBackground(Color.YELLOW);
		messageLabel1.setOpaque(true);
		add(messageLabel1);

		messageLabel2 = new JLabel("공격 몬스터 (╬ಠ益ಠ) : 클릭");
		messageLabel2.setBounds(10, 55, 170, 15);
		messageLabel2.setBackground(Color.YELLOW);
		messageLabel2.setOpaque(true);
		add(messageLabel2);

		messageLabel3 = new JLabel("방어 몬스터 (ಠ_ಠ) : 더블클릭");
		messageLabel3.setBounds(10, 70, 170, 15);
		messageLabel3.setBackground(Color.YELLOW);
		messageLabel3.setOpaque(true);
		add(messageLabel3);

		// 공격 몬스터, 방어 몬스터를 각각 10마리씩 생성
		for (int i = 0; i < 10; i++) {
			AttackMonster attackMonster = new AttackMonster(player); // 공격 몬스터에 플레이어 객체 전달
			DefenseMonster defenseMonster = new DefenseMonster(); // 방어 몬스터

			attackMonster.getLabel().addMouseListener(new MonsterClickListener(attackMonster)); // 라벨을 마우스로 클릭 시 처치되도록 추가
			defenseMonster.getLabel().addMouseListener(new MonsterClickListener(defenseMonster)); // 라벨을 마우스로 클릭 시 처치되도록 추가
         
			add(attackMonster.getLabel()); // 화면에 추가
			add(defenseMonster.getLabel()); // 화면에 추가
		}

		setSize(500, 500); // 500*500으로 크기 설정
		setLayout(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	// 몬스터 클릭 이벤트 처리
	private class MonsterClickListener extends MouseAdapter {
		private MonsterAction monster;

		public MonsterClickListener(MonsterAction monster) {
			this.monster = monster;
		}

		// 오버라이딩 (마우스 클릭 시)
		public void mouseClicked(MouseEvent e) {
			JLabel lb = (JLabel) e.getSource();

			// 방어 몬스터는 더블 클릭으로 처치 가능
			if (lb.isVisible() && lb.getText().equals("(ಠ_ಠ)")) { // 라벨의 이름이 "방어"일 경우
				if (e.getClickCount() == 2) { // 클릭 횟수 받아오기(더블 클릭인지 확인)
					monster.performAction(); // 행동 하기
					count++; // 카운트 증가
					countLabel.setText("몬스터 " + count + "마리 처치!!");
					lb.setVisible(false); // 죽으면 안 보이게
				}
			}
			// 공격 몬스터는 한 번 클릭으로 처치 가능
			else if (lb.isVisible() && lb.getText().equals("(╬ಠ益ಠ)")) { // 라벨의 이름이 "공격"일 경우
				monster.die();
				count++;
				countLabel.setText("몬스터 " + count + "마리 처치!!");
				lb.setVisible(false); // 죽으면 안 보이게
			}
			// 모든 몬스터를 다 잡았을 때
			if (count == TOTAL_MONSTERS) {
				JOptionPane.showMessageDialog(null, "Clear!");
				System.exit(0); // 게임 종료
			}
		}
	}

 	public static void main(String[] args) {
 		new MonsterGame();
 	}
}