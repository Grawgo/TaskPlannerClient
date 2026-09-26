from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent
S = 2
W, H = 390, 844
FONT = '/System/Library/Fonts/Supplemental/Arial.ttf'
BOLD = '/System/Library/Fonts/Supplemental/Arial Bold.ttf'

LIGHT = dict(bg='#F7F7FB', surface='#FFFFFF', text='#1C1B22', muted='#6F6D79',
             primary='#6856A8', soft='#EDE8F8', border='#E6E4ED', red='#B72D3A',
             redsoft='#FFF0F1', green='#2E785B', greensoft='#E4F3EA')
DARK = dict(bg='#15141B', surface='#24232B', text='#F4F2F8', muted='#B5B2C0',
            primary='#B8A7F0', soft='#393346', border='#44414E', red='#FFB4BD',
            redsoft='#4A2C34', green='#A7DFC1', greensoft='#2A4438')

class Screen:
    def __init__(self, palette=LIGHT, status_dark=False):
        self.p = palette
        self.im = Image.new('RGB', (W*S, H*S), palette['bg'])
        self.d = ImageDraw.Draw(self.im)
        self.status_dark = status_dark
    def font(self, n, bold=False):
        return ImageFont.truetype(BOLD if bold else FONT, n*S)
    def rect(self, x0,y0,x1,y1,fill,r=0,outline=None,width=1):
        box=(int(x0*S),int(y0*S),int(x1*S),int(y1*S))
        if r:
            self.d.rounded_rectangle(box, radius=int(r*S), fill=fill,
                outline=outline, width=int(width*S))
        else:
            self.d.rectangle(box, fill=fill, outline=outline, width=int(width*S))
    def line(self, points, fill, width=2):
        self.d.line([(int(x*S),int(y*S)) for x,y in points], fill=fill, width=int(width*S), joint='curve')
    def circle(self,x,y,r,fill,outline=None,width=1):
        self.d.ellipse(((x-r)*S,(y-r)*S,(x+r)*S,(y+r)*S),fill=fill,outline=outline,width=int(width*S))
    def text(self,x,y,value,size=16,fill=None,bold=False,anchor=None):
        self.d.text((x*S,y*S),value,font=self.font(size,bold),fill=fill or self.p['text'],anchor=anchor)
    def wrapped(self,x,y,value,max_width,size=15,fill=None,line_h=22):
        words=value.split(); lines=[]; current=''
        for word in words:
            trial=f'{current} {word}'.strip()
            if self.d.textbbox((0,0),trial,font=self.font(size))[2] <= max_width*S:
                current=trial
            else:
                if current: lines.append(current)
                current=word
        if current: lines.append(current)
        for n,line in enumerate(lines): self.text(x,y+n*line_h,line,size,fill)
        return len(lines)*line_h
    def save(self,name):
        self.im.save(OUT/name,optimize=True)
    def status(self,light=False):
        c='#FFFFFF' if light else self.p['text']
        self.text(24,17,'9:41',14,c,True)
        self.line([(326,28),(329,24),(332,28)],c,2)
        self.line([(338,28),(342,22),(346,28)],c,2)
        self.rect(354,20,373,30,None,3,c,1)
        self.rect(356,22,368,28,c,2)
        self.rect(373,23,375,27,c,1)
    def search_icon(self,x,y,color=None):
        c=color or self.p['muted']
        self.circle(x,y,7,None,c,2)
        self.line([(x+5,y+5),(x+12,y+12)],c,2)
    def back(self,x=28,y=74):
        c=self.p['text']; self.line([(x+8,y-7),(x,y),(x+8,y+7)],c,2)
    def x_icon(self,x,y,color=None):
        c=color or self.p['muted']; self.line([(x-5,y-5),(x+5,y+5)],c,2); self.line([(x+5,y-5),(x-5,y+5)],c,2)
    def check(self,x,y,color=None,filled=False):
        c=color or self.p['primary']
        self.circle(x,y,10,c if filled else None,c,2)
        if filled:self.line([(x-5,y),(x-1,y+4),(x+6,y-5)],'#FFFFFF',2)
    def field(self,y,label,value='',hint='',password=False):
        self.text(24,y,label,13,self.p['muted'])
        self.rect(24,y+24,366,y+79,self.p['surface'],15,self.p['border'])
        self.text(40,y+40,('••••••••' if password and value else value or hint),16,
                  self.p['text'] if value else self.p['muted'])
    def button(self,y,label,secondary=False):
        bg=self.p['soft'] if secondary else self.p['primary']
        fg=self.p['primary'] if secondary else ('#1C1B22' if self.p is DARK else '#FFFFFF')
        self.rect(24,y,366,y+54,bg,17)
        self.text(195,y+27,label,16,fg,True,'mm')
    def nav(self,active='Задачи'):
        self.rect(0,772,W,H,self.p['surface'])
        self.line([(0,772),(W,772)],self.p['border'],1)
        centers=[65,195,325]; labels=['Задачи','Поиск','Настройки']
        for x,l in zip(centers,labels):
            c=self.p['primary'] if l==active else self.p['muted']
            if l=='Задачи':
                self.rect(x-10,790,x+10,808,None,4,c,2)
                self.line([(x-5,795),(x+4,795)],c,2); self.line([(x-5,801),(x+4,801)],c,2)
            elif l=='Поиск': self.search_icon(x-2,798,c)
            else:
                self.circle(x,798,9,None,c,2); self.circle(x,798,3,None,c,2)
            self.text(x,818,l,11,c,l==active,'mm')
    def search_field(self,y,value='',hint='Найти задачу'):
        self.rect(20,y,370,y+54,self.p['surface'],17,self.p['border'])
        self.search_icon(44,y+25)
        self.text(68,y+17,value or hint,16,self.p['text'] if value else self.p['muted'])
        if value:self.x_icon(340,y+27)
    def pill(self,x,y,w,label,kind='soft'):
        bg=self.p.get(kind,self.p['soft']); fg=self.p['primary']
        if kind=='redsoft':fg=self.p['red']
        if kind=='greensoft':fg=self.p['green']
        self.rect(x,y,x+w,y+30,bg,15)
        self.text(x+w/2,y+15,label,12,fg,True,'mm')
    def task_card(self,y,title,desc,when,priority,status='TODO',overdue=False,dark=False):
        self.rect(20,y,370,y+132,self.p['surface'],20)
        self.check(46,y+33,filled=status=='DONE')
        self.text(67,y+20,title,16,self.p['text'],True)
        self.text(67,y+51,desc,13,self.p['muted'])
        self.line([(67,y+87),(343,y+87)],self.p['border'],1)
        self.text(67,y+103,when,12,self.p['red'] if overdue else self.p['muted'])
        kind='redsoft' if priority=='Высокий' else ('greensoft' if priority=='Низкий' else 'soft')
        self.pill(270,y+97,78,priority,kind)

def login():
    s=Screen(); s.rect(0,0,390,285,'#6856A8'); s.status(True)
    s.circle(64,103,29,'#8D7BC1'); s.check(64,103,'#6856A8',True)
    s.text(24,155,'Планируй спокойно',27,'#FFFFFF',True)
    s.text(24,196,'Все задачи в одном месте',15,'#EEE9FC')
    s.rect(0,260,390,844,LIGHT['bg'],28)
    s.text(24,285,'Вход',25,bold=True)
    s.text(24,322,'Рады видеть вас снова',14,LIGHT['muted'])
    s.field(363,'Электронная почта',hint='name@example.com')
    s.field(462,'Пароль',hint='Введите пароль')
    s.text(366,564,'Забыли пароль?',13,LIGHT['primary'],True,'ra')
    s.button(606,'Войти')
    s.text(195,706,'Нет аккаунта?',14,LIGHT['muted'],anchor='mm')
    s.text(195,737,'Зарегистрироваться',15,LIGHT['primary'],True,'mm')
    s.save('01-login.png')

def register():
    s=Screen(); s.status(); s.back(); s.text(24,120,'Создать аккаунт',27,bold=True)
    s.text(24,161,'Начните планировать задачи',14,LIGHT['muted'])
    s.field(210,'Электронная почта',hint='name@example.com')
    s.field(309,'Пароль',hint='Не менее 6 символов')
    s.field(408,'Повторите пароль',hint='Введите пароль ещё раз')
    s.button(540,'Зарегистрироваться')
    s.text(195,640,'Уже есть аккаунт?',14,LIGHT['muted'],anchor='mm')
    s.text(195,671,'Войти',15,LIGHT['primary'],True,'mm')
    s.save('02-register.png')

def task_list(dark=False):
    p=DARK if dark else LIGHT; s=Screen(p); s.status()
    s.text(20,70,'Мои задачи',27,bold=True)
    s.text(20,109,'Все ваши планы под рукой',14,p['muted'])
    s.search_field(148)
    s.pill(20,218,64,'Все'); s.pill(92,218,93,'Активные','greensoft'); s.pill(193,218,75,'Готово','soft')
    s.text(20,270,'Ближайшие',17,bold=True)
    s.text(370,273,'Срок ↑',12,p['muted'],anchor='ra')
    s.task_card(306,'Подготовить доклад','Собрать материалы и слайды','2 окт, 18:00','Высокий')
    s.task_card(450,'Прочитать главу 3','Сделать краткие заметки','4 окт, 20:00','Средний')
    s.task_card(594,'Отправить письмо','Руководителю курсовой','Вчера, 16:00','Низкий',overdue=True)
    s.circle(331,727,29,p['primary']); s.line([(319,727),(343,727)],'#FFFFFF' if not dark else p['bg'],3); s.line([(331,715),(331,739)],'#FFFFFF' if not dark else p['bg'],3)
    s.nav('Задачи'); s.save('11-task-list-dark.png' if dark else '03-task-list.png')

def task_list_empty():
    s=Screen(); s.status()
    s.text(20,70,'Мои задачи',27,bold=True)
    s.text(20,109,'Все ваши планы под рукой',14,LIGHT['muted'])
    s.search_field(148)
    s.pill(20,218,64,'Все'); s.pill(92,218,93,'Активные','greensoft'); s.pill(193,218,75,'Готово')
    s.circle(195,389,65,LIGHT['soft'])
    s.rect(177,368,213,408,None,7,LIGHT['primary'],2)
    s.line([(184,382),(206,382)],LIGHT['primary'],2)
    s.line([(184,392),(200,392)],LIGHT['primary'],2)
    s.text(195,496,'Задач пока нет',21,LIGHT['text'],True,'mm')
    s.text(195,530,'Создайте первую задачу, чтобы начать',14,LIGHT['muted'],anchor='mm')
    s.button(591,'Создать задачу')
    s.nav('Задачи'); s.save('12-task-list-empty.png')

def search_base(value=''):
    s=Screen(); s.status(); s.text(20,70,'Поиск',27,bold=True); s.search_field(119,value)
    s.nav('Поиск'); return s

def search_history():
    s=search_base(); s.text(20,205,'Недавние запросы',17,bold=True)
    s.text(370,209,'Очистить историю',12,LIGHT['primary'],True,'ra')
    for y,q in [(255,'доклад'),(318,'письмо'),(381,'глава 3')]:
        s.rect(20,y,370,y+54,LIGHT['surface'],15)
        s.search_icon(43,y+25); s.text(66,y+16,q,15)
    s.save('04-search-history.png')

def search_results():
    s=search_base('доклад'); s.text(20,205,'Найдено 2 задачи',15,LIGHT['muted'])
    s.task_card(245,'Подготовить доклад','Собрать материалы и слайды','2 окт, 18:00','Высокий')
    s.task_card(391,'Проверить доклад','Исправить замечания','5 окт, 12:00','Средний')
    s.save('05-search-results.png')

def search_loading():
    s=search_base('доклад')
    s.circle(195,376,49,LIGHT['soft'])
    s.d.arc((174*S,355*S,216*S,397*S),45,305,fill=LIGHT['primary'],width=5*S)
    s.text(195,461,'Ищем задачи…',19,LIGHT['text'],True,'mm')
    s.save('13-search-loading.png')

def search_empty():
    s=search_base('несуществующая задача'); s.circle(195,364,65,LIGHT['soft'])
    s.search_icon(189,358,LIGHT['primary'])
    s.text(195,472,'Ничего не найдено',21,LIGHT['text'],True,'mm')
    s.wrapped(48,505,'Попробуйте изменить запрос или проверить его написание',300,14,LIGHT['muted'],21)
    s.save('06-search-empty.png')

def search_error():
    s=search_base('доклад'); s.circle(195,357,65,LIGHT['redsoft'])
    s.text(195,359,'!',45,LIGHT['red'],True,'mm')
    s.text(195,464,'Не удалось выполнить поиск',19,LIGHT['text'],True,'mm')
    s.text(195,503,'Проверьте подключение и повторите',14,LIGHT['muted'],anchor='mm')
    s.button(554,'Обновить')
    s.save('07-search-error.png')

def task_detail():
    s=Screen(); s.status(); s.back(); s.text(64,61,'Задача',19,bold=True)
    s.text(365,67,'•••',22,LIGHT['muted'],anchor='ra')
    s.pill(24,132,96,'В работе','soft')
    s.wrapped(24,190,'Подготовить доклад',335,27,LIGHT['text'],32)
    s.text(24,251,'Описание',14,LIGHT['muted'])
    s.wrapped(24,283,'Собрать материалы и слайды для защиты курсовой работы.',335,16,LIGHT['text'],24)
    s.rect(20,375,370,509,LIGHT['surface'],19)
    s.text(42,402,'Срок выполнения',13,LIGHT['muted'])
    s.text(42,432,'2 октября, 18:00',17,bold=True)
    s.line([(42,468),(348,468)],LIGHT['border'],1)
    s.text(42,481,'Приоритет',13,LIGHT['muted']); s.text(348,481,'Высокий',13,LIGHT['red'],True,'ra')
    s.button(590,'Отметить выполненной')
    s.button(656,'Редактировать',True)
    s.text(195,747,'Удалить задачу',14,LIGHT['red'],True,'mm')
    s.save('08-task-detail.png')

def editor():
    s=Screen(); s.status(); s.back(); s.text(64,61,'Новая задача',19,bold=True)
    s.field(122,'Название',hint='Что нужно сделать?')
    s.text(24,228,'Описание',13,LIGHT['muted'])
    s.rect(24,253,366,363,LIGHT['surface'],15,LIGHT['border'])
    s.text(40,269,'Добавьте подробности',15,LIGHT['muted'])
    s.field(393,'Срок',hint='Выберите дату и время')
    s.text(24,501,'Приоритет',13,LIGHT['muted'])
    s.pill(24,533,84,'Низкий','greensoft'); s.pill(116,533,91,'Средний','soft'); s.pill(215,533,92,'Высокий','redsoft')
    s.text(24,590,'Статус',13,LIGHT['muted'])
    s.pill(24,622,122,'К выполнению','soft')
    s.button(705,'Сохранить задачу')
    s.save('09-task-editor.png')

def settings():
    s=Screen(); s.status(); s.text(20,70,'Настройки',27,bold=True)
    s.rect(20,137,370,235,LIGHT['surface'],20)
    s.circle(68,186,27,LIGHT['soft']); s.text(68,185,'А',23,LIGHT['primary'],True,'mm')
    s.text(111,162,'Мой аккаунт',16,bold=True); s.text(111,190,'student@example.com',13,LIGHT['muted'])
    s.text(20,272,'Оформление',17,bold=True)
    s.rect(20,311,370,384,LIGHT['surface'],20)
    s.text(43,331,'Тёмная тема',16,bold=True); s.text(43,356,'Изменить цвета приложения',12,LIGHT['muted'])
    s.rect(305,331,348,355,LIGHT['border'],13); s.circle(318,343,9,LIGHT['surface'])
    s.text(20,434,'Аккаунт',17,bold=True)
    s.rect(20,473,370,536,LIGHT['surface'],20)
    s.text(43,494,'Выйти из аккаунта',16,LIGHT['red'],True)
    s.nav('Настройки'); s.save('10-settings.png')

for fn in (login,register,task_list,search_history,search_results,search_empty,search_error,task_detail,editor,settings,task_list_empty,search_loading): fn()
task_list(True)

names=['01-login.png','02-register.png','03-task-list.png','04-search-history.png',
       '05-search-results.png','06-search-empty.png','07-search-error.png',
       '08-task-detail.png','09-task-editor.png','10-settings.png','11-task-list-dark.png',
       '12-task-list-empty.png','13-search-loading.png']
thumb_w,thumb_h=390,844
pad=24; heading=44; cols=4; rows=(len(names)+cols-1)//cols
sheet=Image.new('RGB',(cols*thumb_w+(cols+1)*pad, rows*(thumb_h+heading)+(rows+1)*pad),'#ECEAF1')
d=ImageDraw.Draw(sheet)
font=ImageFont.truetype(BOLD,17)
for i,name in enumerate(names):
    col,row=i%cols,i//cols
    x=pad+col*(thumb_w+pad); y=pad+row*(thumb_h+heading+pad)
    d.text((x,y+5),name.removesuffix('.png').replace('-',' '),font=font,fill='#26212F')
    im=Image.open(OUT/name).resize((thumb_w,thumb_h),Image.Resampling.LANCZOS)
    sheet.paste(im,(x,y+heading))
sheet.save(OUT/'overview.png',optimize=True)
print('Generated',len(names),'screens and overview')
