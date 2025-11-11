import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

interface MonsterAction {
    void action();
}

class Monster {
    String name;
    private int x, y;

    public Monster(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

class AttackMonster extends Monster implements MonsterAction {
    private Container contentPane;
    private List<AttackMonster> attackMonsters;  // AttackMonster 리스트 추가

    public AttackMonster(Container contentPane, List<AttackMonster> attackMonsters, String name, int x, int y) {
        super(name, x, y);
        this.contentPane = contentPane;
        this.attackMonsters = attackMonsters;  // 리스트 초기화
    }

    @Override
    public void action() {
        // 2초마다 랜덤하게 위치 바꾸기
        synchronized (contentPane) {
            final int[] newPosition = new int[2];

            do {
                newPosition[0] = (int) (Math.random() * (contentPane.getWidth() - 80));
                newPosition[1] = (int) (Math.random() * (contentPane.getHeight() - 30));

                newPosition[0] = Math.max(0, Math.min(newPosition[0], contentPane.getWidth() - 80));
                newPosition[1] = Math.max(0, Math.min(newPosition[1], contentPane.getHeight() - 30));

                // 랜덤 위치 생성값 출력
                System.out.println("New Position: (" + newPosition[0] + ", " + newPosition[1] + ")");
            } while (isTooCloseToOtherMonsters(newPosition[0], newPosition[1]));

            JLabel label = findLabel(contentPane, this);
            if (label != null) {
                // SwingUtilities.invokeLater를 사용하여 메인 스레드에서 실행
                SwingUtilities.invokeLater(() -> {
                    label.setLocation(newPosition[0], newPosition[1]);
                    contentPane.repaint();
                });
            }
        }
    }

    private boolean isTooCloseToOtherMonsters(int x, int y) {
        for (AttackMonster attackMonster : attackMonsters) {
            // 기존 몬스터들과의 거리를 확인하여 가까우면 true 반환
            if (Math.abs(x - attackMonster.getX()) < 50 && Math.abs(y - attackMonster.getY()) < 50) {
                return true;
            }
        }
        return false;
    }

    private JLabel findLabel(Container contentPane, Monster monster) {
        Component[] components = contentPane.getComponents();
        for (Component component : components) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getText().equals(monster.name)) {
                    return label;
                }
            }
        }
        return null;
    }
}


class DefenseMonster extends Monster implements MonsterAction {
    private Container contentPane;
    private MonsterAction monsterAction;
    private Final finalInstance; // 필드 추가

    public DefenseMonster(Container contentPane, Final finalInstance, String name, int x, int y, MonsterAction monsterAction) {
        super(name, x, y);
        this.contentPane = contentPane;
        this.finalInstance = finalInstance; // 필드 초기화 추가
        this.monsterAction = monsterAction;
    }

    @Override
    public void action() {
        // 한마리씩 추가
        synchronized (this) {
            addMonster(); // contentPane를 전달하지 않고 내부에서 사용
        }
    }

    public void addMonster() {
        synchronized (contentPane) {
            int newX = (int) (Math.random() * (contentPane.getWidth() - 80));
            int newY = (int) (Math.random() * (contentPane.getHeight() - 30));

            newX = Math.max(0, Math.min(newX, contentPane.getWidth() - 80));
            newY = Math.max(0, Math.min(newY, contentPane.getHeight() - 30));

            JLabel Dlabel = new JLabel(name);

            Dlabel.setSize(80, 30);
            Dlabel.setLocation(newX, newY);
            Dlabel.setBackground(Color.YELLOW);
            Dlabel.setOpaque(true);

            Dlabel.addMouseListener(new MonsterThread.MyMouseListener(finalInstance));

            contentPane.add(Dlabel);
            contentPane.repaint();
            contentPane.notifyAll();
        }
    }
}

class MonsterThread extends Thread {
    private Container contentPane;
    private volatile boolean flag = false;
    private static int count = 0; // 몬스터 처치 횟수
    private Final finalInstance; // Final 클래스 인스턴스
    private List<AttackMonster> attackMonsters = new ArrayList<>(); // AttackMonster 리스트 추가

    public MonsterThread(Container contentPane, Final finalInstance) {
        this.contentPane = contentPane;
        this.finalInstance = finalInstance;
    }

    void finish() {
        flag = true;
        synchronized (contentPane) {
            contentPane.notifyAll(); // 다른 스레드가 대기 중인 경우 깨움
        }
    }

    public void run() {
        addMonsters(10);

        while (!flag) {
            try {
                Thread.sleep(500);
                addDefenseMonster();
                updateAttackMonsters();
                
                synchronized (contentPane) {
                    // 이 조건을 만족할 때까지 대기
                    while (!flag && attackMonsters.size() < 10) {
                        contentPane.wait();
                    }
                }
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }


    // 방어 몬스터 생성
    void addDefenseMonster() {
        synchronized (contentPane) {
        	int newX = (int)(Math.random()*(contentPane.getWidth() - 80));
        	int newY = (int)(Math.random()*(contentPane.getHeight() - 30));
        	
        	JLabel Dlabel = new JLabel("방어");
        	Dlabel.setSize(80, 30);
        	Dlabel.setLocation(newX, newY);
        	Dlabel.setBackground(Color.YELLOW);
        	Dlabel.setOpaque(true);
        	Dlabel.addMouseListener(new MonsterThread.MyMouseListener(finalInstance));
        	
        	contentPane.add(Dlabel);
        	contentPane.repaint();
        	contentPane.notifyAll();
        }
    }

    // 공격 몬스터 위치 업데이트
    private void updateAttackMonsters() {
    	SwingUtilities.invokeLater(() -> {
            synchronized (contentPane) {
                for (AttackMonster attackMonster : attackMonsters) {
                    attackMonster.action();
                }
            }
        });
    }

    // 초기 실행 시 몬스터 생성
    private void addMonsters(int count) {
        synchronized (contentPane) {
            for (int i = 0; i < count; i++) {
                AttackMonster Amonster = new AttackMonster(contentPane, attackMonsters, "공격", (int) (Math.random() * (contentPane.getWidth() - 80)),
                        (int) (Math.random() * (contentPane.getHeight() - 30)));
                DefenseMonster Dmonster = new DefenseMonster(contentPane, finalInstance, "방어", (int) (Math.random() * (contentPane.getWidth() - 80)),
                        (int) (Math.random() * (contentPane.getHeight() - 30)), null);

                JLabel Alabel = new JLabel(Amonster.name);
                JLabel Dlabel = new JLabel(Dmonster.name);

                Alabel.setSize(80, 30);
                Dlabel.setSize(80, 30);

                Alabel.setLocation(Amonster.getX(), Amonster.getY());
                Dlabel.setLocation(Dmonster.getX(), Dmonster.getY());

                Alabel.setBackground(Color.YELLOW);
                Dlabel.setBackground(Color.YELLOW);

                Alabel.setOpaque(true);
                Dlabel.setOpaque(true);

                Alabel.addMouseListener(new MyMouseListener(finalInstance));
                Dlabel.addMouseListener(new MyMouseListener(finalInstance));

                contentPane.add(Alabel);
                contentPane.add(Dlabel);
                
             // AttackMonster를 리스트에 추가
                attackMonsters.add(Amonster);
            }
            
            contentPane.repaint();
            contentPane.notifyAll(); // 대기 중인 스레드에게 변경 사항 알림
        }
    }

    // 마우스로 몬스터 처치 시 카운트 증가
    static class MyMouseListener extends MouseAdapter {
        private Final finalInstance;

        public MyMouseListener(Final finalInstance) {
            this.finalInstance = finalInstance;
        }

        public void mouseClicked(MouseEvent e) {
            JLabel lb = (JLabel) e.getSource();

            if (lb.isVisible() && lb.getText().equals("방어")) {
                if (e.getClickCount() == 2) { // 더블클릭 이벤트 처리
                    count++;
                    finalInstance.setTitle("몬스터 " + count + "마리 처치!!");
                    lb.setVisible(false);
                }
            } else if (lb.isVisible() && lb.getText().equals("공격")) {
                // 공격 몬스터는 한 번의 클릭으로 사라지게 함
                count++;
                finalInstance.setTitle("몬스터 " + count + "마리 처치!!");
                lb.setVisible(false);
            }
        }
    }
}

public class Final extends JFrame {
    private int count = 0;

    public Final() {
        setTitle("몬스터를 처치하세요!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container c = getContentPane();
        c.setLayout(null);

        setSize(500, 500);
        setVisible(true);

        MonsterThread th = new MonsterThread(c, this);
        th.start();

        c.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                th.finish();
            }
        });
    }

    public static void main(String[] args) {
        new Final();
    }
}