import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
import pandas as pd
import itertools
from typing import List, Dict, Tuple
import time
import multiprocessing
import json
import sys
from pathlib import Path
# 技能分值映射
skill_score_map = {
    'C':7,
    'B':12,
    'A':17,
    'S':22
}
# 图片统一缩放大小
PET_ICON_SIZE = 64
# 每行固定显示10个宠物
COLS_PER_ROW = 10
MIN_PET_LIST_HEIGHT = 120
MAX_PETS_PER_TASK = 3
MAX_BORROWED_PER_TASK = 1
MAX_TOTAL_BORROWED = 3
SPECIAL_TIER_RANK = 5
TIER_NAME_MAP = {
    5: "特阶",
    4: "一阶",
    3: "二阶",
    2: "三阶",
    1: "四阶",
    0: "无奖励"
}
SELECTION_STATE_FILE = "dispatch_selection_state.json"


def get_app_base_path() -> Path:
    if getattr(sys, "frozen", False):
        return Path(sys.executable).resolve().parent
    return Path(__file__).resolve().parent


def get_selection_state_path() -> Path:
    return get_app_base_path() / SELECTION_STATE_FILE


def load_selection_state() -> Dict:
    state_path = get_selection_state_path()
    if not state_path.exists():
        return {}

    try:
        with state_path.open("r", encoding="utf-8") as file:
            data = json.load(file)
    except (OSError, json.JSONDecodeError):
        return {}

    return data if isinstance(data, dict) else {}


def save_selection_state(state: Dict):
    state_path = get_selection_state_path()
    with state_path.open("w", encoding="utf-8") as file:
        json.dump(state, file, ensure_ascii=False, indent=2)


def check_dependencies():
    try:
        import pandas
        import openpyxl
        from PIL import Image, ImageTk
    except ImportError:
        return False
    return True
def read_pet_list() -> List[Dict]:
    """读取宠物列表.xlsx文件"""
    try:
        df = pd.read_excel('./data/宠物列表.xlsx', sheet_name='Sheet1')
    except FileNotFoundError:
        raise FileNotFoundError("未找到./data/宠物列表.xlsx文件，请确保文件存在于data文件夹中。")
    pets = []
    # 稀有度基础分映射
    rarity_base_map = {
        '普通宠物':2,
        '高级宠物':2,
        '稀有宠物':3,
        '传说宠物':5
    }
    for idx, row in df.iterrows():
        pet_name = row.iloc[0]
        if pd.isna(pet_name):
            continue
        rarity = row.iloc[1]
        skill1 = row.iloc[2]
        skill1_level = row.iloc[3]
        skill2 = row.iloc[4]
        skill2_level = row.iloc[5]
        base_score = rarity_base_map.get(rarity, 2)
        skills = {}
        if pd.notna(skill1) and pd.notna(skill1_level):
            skills[skill1] = skill_score_map.get(skill1_level, 0)
        if pd.notna(skill2) and pd.notna(skill2_level):
            skills[skill2] = skill_score_map.get(skill2_level, 0)
        pets.append({
            'id': idx+1,
            'name': pet_name,
            'rarity': rarity,
            'base_score': base_score,
            'skills': skills,
            'is_borrowed': False  # 默认不是借用的
        })
    return pets
def read_regions() -> Dict[str, List[Dict]]:
    """读取跑腿地区.xlsx文件"""
    try:
        df = pd.read_excel('./data/跑腿地区.xlsx', sheet_name='Sheet1')
    except FileNotFoundError:
        raise FileNotFoundError("未找到./data/跑腿地区.xlsx文件，请确保文件存在于data文件夹中。")
    regions = {}
    current_region = None
    for idx, row in df.iterrows():
        region_name = row.iloc[0]
        if pd.notna(region_name):
            current_region = region_name
            if current_region not in regions:
                regions[current_region] = []
        if current_region is None:
            continue
        area = row.iloc[1]
        task = row.iloc[2]
        bonus1 = row.iloc[3]
        bonus2 = row.iloc[4]
        bonus_skills = []
        if pd.notna(bonus1):
            bonus_skills.append(bonus1)
        if pd.notna(bonus2):
            bonus_skills.append(bonus2)
        if pd.notna(area) and pd.notna(task):
            regions[current_region].append({
                'area': area,
                'task': task,
                'bonus_skills': bonus_skills,
                'id': len(regions[current_region])  # 任务ID
            })
    # 确保每个区域有5个任务，不足的用空任务填充
    for region in regions:
        while len(regions[region]) <5:
            regions[region].append({
                'area': '',
                'task': '',
                'bonus_skills': [],
                'id': len(regions[region])
            })
    return regions
def precompute_pet_task_scores(pets: List[Dict], tasks: List[Dict]) -> Dict[int, Dict[int, int]]:
    """预计算每个宠物对每个任务的得分"""
    pet_task_scores = {}
    for pet in pets:
        pet_scores = {}
        for task in tasks:
            total = 0
            for skill, score in pet['skills'].items():
                if skill in task['bonus_skills']:
                    total += score
            pet_scores[task['id']] = total if total !=0 else pet['base_score']
        pet_task_scores[pet['id']] = pet_scores
    return pet_task_scores


def calculate_team_score(combo: List[Dict], task: Dict, pet_task_scores: Dict[int, Dict[int, int]]) -> int:
    """计算宠物组合的总得分"""
    return sum(pet_task_scores[pet['id']][task['id']] for pet in combo)


def get_reward_tier_rank(score: int) -> int:
    """根据得分返回奖励档位排序值。值越大档位越高。"""
    if score > 37:
        return 5
    if score > 25:
        return 4
    if score > 13:
        return 3
    if score > 5:
        return 2
    if score > 1:
        return 1
    return 0


def get_reward_level(score: int) -> str:
    """根据总得分获取奖励等级"""
    return TIER_NAME_MAP[get_reward_tier_rank(score)]


def get_tier_counts_for_rank(tier_rank: int) -> Tuple[int, int, int, int, int]:
    if tier_rank == 0:
        return (0, 0, 0, 0, 0)
    counts = [0, 0, 0, 0, 0]
    counts[SPECIAL_TIER_RANK - tier_rank] = 1
    return tuple(counts)


def add_tier_counts(
    left: Tuple[int, int, int, int, int],
    right: Tuple[int, int, int, int, int]
) -> Tuple[int, int, int, int, int]:
    return tuple(a + b for a, b in zip(left, right))


def build_objective(
    tier_counts: Tuple[int, int, int, int, int],
    borrowed_total: int,
    total_pets: int,
    total_score: int
) -> Tuple[int, int, int, int, int, int, int, int]:
    return tuple(tier_counts) + (-borrowed_total, -total_pets, total_score)


def build_prefix_objective(
    tier_counts: Tuple[int, int, int, int, int],
    total_pets: int,
    total_score: int
) -> Tuple[int, int, int, int, int, int, int]:
    return tuple(tier_counts) + (-total_pets, total_score)


def is_candidate_better(candidate: Dict, existing: Dict) -> bool:
    return (candidate['tier_rank'], candidate['score']) > (existing['tier_rank'], existing['score'])


def build_task_candidates(task: Dict, available_pets: List[Dict], pet_task_scores: Dict[int, Dict[int, int]]) -> Dict:
    best_by_signature = {}
    for team_size in range(1, MAX_PETS_PER_TASK + 1):
        if len(available_pets) < team_size:
            continue
        for combo in itertools.combinations(available_pets, team_size):
            borrowed_count = sum(1 for pet in combo if pet.get('is_borrowed', False))
            if borrowed_count > MAX_BORROWED_PER_TASK:
                continue

            pet_names = [pet['name'] for pet in combo]
            if len(set(pet_names)) != len(pet_names):
                continue

            owned_mask = 0
            for pet in combo:
                if not pet.get('is_borrowed', False):
                    owned_mask |= 1 << (pet['id'] - 1)

            score = calculate_team_score(list(combo), task, pet_task_scores)
            tier_rank = get_reward_tier_rank(score)
            candidate = {
                'team': list(combo),
                'owned_mask': owned_mask,
                'borrowed_count': borrowed_count,
                'pet_count': team_size,
                'score': score,
                'tier_rank': tier_rank,
                'tier_counts': get_tier_counts_for_rank(tier_rank)
            }

            signature = (owned_mask, borrowed_count, team_size)
            existing = best_by_signature.get(signature)
            if existing is None or is_candidate_better(candidate, existing):
                best_by_signature[signature] = candidate

    candidates = list(best_by_signature.values())
    candidates.sort(key=lambda item: (-item['tier_rank'], item['pet_count'], item['borrowed_count'], -item['score']))
    special_candidates = [candidate for candidate in candidates if candidate['tier_rank'] == SPECIAL_TIER_RANK]
    special_candidates.sort(key=lambda item: (item['pet_count'], item['borrowed_count'], -item['score']))

    best_tier_rank = max((candidate['tier_rank'] for candidate in candidates), default=0)
    return {
        'task': task,
        'candidates': candidates,
        'special_candidates': special_candidates,
        'best_tier_rank': best_tier_rank,
        'best_counts': get_tier_counts_for_rank(best_tier_rank),
        'max_score': max((candidate['score'] for candidate in candidates), default=0),
        'min_pet_count': min((candidate['pet_count'] for candidate in candidates), default=0),
        'min_borrowed': min((candidate['borrowed_count'] for candidate in candidates), default=0)
    }


def build_task_entries(task_combination: Tuple[Dict], available_pets: List[Dict], pet_task_scores: Dict[int, Dict[int, int]]) -> List[Dict]:
    task_entries = []
    for original_index, task in enumerate(task_combination):
        task_entry = build_task_candidates(task, available_pets, pet_task_scores)
        task_entry['original_index'] = original_index
        task_entries.append(task_entry)
    return task_entries


def get_task_search_order(task_entries: List[Dict]) -> List[Dict]:
    return sorted(
        task_entries,
        key=lambda entry: (
            len(entry['special_candidates']),
            len(entry['candidates']),
            -entry['best_tier_rank'],
            -entry['max_score']
        )
    )


def build_solution_from_pairs(task_count: int, chosen_pairs: List[Tuple[Dict, Dict]]) -> Dict:
    assignments = [None] * task_count
    total_score = 0
    borrowed_total = 0
    total_pets = 0
    tier_counts = (0, 0, 0, 0, 0)

    for task_entry, candidate in chosen_pairs:
        assignments[task_entry['original_index']] = {
            'task': task_entry['task'],
            'team': candidate['team'],
            'score': candidate['score']
        }
        total_score += candidate['score']
        borrowed_total += candidate['borrowed_count']
        total_pets += candidate['pet_count']
        tier_counts = add_tier_counts(tier_counts, candidate['tier_counts'])

    return {
        'assignments': assignments,
        'total': total_score,
        'borrowed': borrowed_total,
        'total_pets': total_pets,
        'tier_counts': tier_counts,
        'objective': build_objective(tier_counts, borrowed_total, total_pets, total_score),
        'all_special': tier_counts[0] == task_count
    }


def search_all_special(task_entries: List[Dict]) -> Dict:
    if any(not entry['special_candidates'] for entry in task_entries):
        return None

    ordered_tasks = get_task_search_order(task_entries)
    chosen_pairs = [None] * len(ordered_tasks)
    failed_states = set()

    def dfs(task_index: int, used_owned_mask: int, borrowed_total: int) -> bool:
        if task_index == len(ordered_tasks):
            return True

        state = (task_index, used_owned_mask, borrowed_total)
        if state in failed_states:
            return False

        task_entry = ordered_tasks[task_index]
        for candidate in task_entry['special_candidates']:
            if borrowed_total + candidate['borrowed_count'] > MAX_TOTAL_BORROWED:
                continue
            if candidate['owned_mask'] & used_owned_mask:
                continue

            chosen_pairs[task_index] = (task_entry, candidate)
            if dfs(
                task_index + 1,
                used_owned_mask | candidate['owned_mask'],
                borrowed_total + candidate['borrowed_count']
            ):
                return True

        failed_states.add(state)
        return False

    if not dfs(0, 0, 0):
        return None

    return build_solution_from_pairs(len(task_entries), chosen_pairs)


def search_best_assignment(task_entries: List[Dict]) -> Dict:
    if any(not entry['candidates'] for entry in task_entries):
        return None

    ordered_tasks = get_task_search_order(task_entries)
    task_count = len(ordered_tasks)
    suffix_best_counts = [(0, 0, 0, 0, 0) for _ in range(task_count + 1)]
    suffix_min_borrowed = [0] * (task_count + 1)
    suffix_min_pets = [0] * (task_count + 1)
    suffix_max_score = [0] * (task_count + 1)

    for index in range(task_count - 1, -1, -1):
        task_entry = ordered_tasks[index]
        suffix_best_counts[index] = add_tier_counts(task_entry['best_counts'], suffix_best_counts[index + 1])
        suffix_min_borrowed[index] = task_entry['min_borrowed'] + suffix_min_borrowed[index + 1]
        suffix_min_pets[index] = task_entry['min_pet_count'] + suffix_min_pets[index + 1]
        suffix_max_score[index] = task_entry['max_score'] + suffix_max_score[index + 1]

    chosen_pairs = [None] * task_count
    best_solution = None
    prefix_cache = {}

    def dfs(
        task_index: int,
        used_owned_mask: int,
        borrowed_total: int,
        tier_counts: Tuple[int, int, int, int, int],
        total_pets: int,
        total_score: int
    ):
        nonlocal best_solution

        optimistic_counts = add_tier_counts(tier_counts, suffix_best_counts[task_index])
        optimistic_objective = build_objective(
            optimistic_counts,
            borrowed_total + suffix_min_borrowed[task_index],
            total_pets + suffix_min_pets[task_index],
            total_score + suffix_max_score[task_index]
        )
        if best_solution is not None and optimistic_objective <= best_solution['objective']:
            return

        state = (task_index, used_owned_mask, borrowed_total)
        prefix_objective = build_prefix_objective(tier_counts, total_pets, total_score)
        cached_prefix = prefix_cache.get(state)
        if cached_prefix is not None and prefix_objective <= cached_prefix:
            return
        prefix_cache[state] = prefix_objective

        if task_index == task_count:
            solution = build_solution_from_pairs(task_count, chosen_pairs)
            if best_solution is None or solution['objective'] > best_solution['objective']:
                best_solution = solution
            return

        task_entry = ordered_tasks[task_index]
        for candidate in task_entry['candidates']:
            next_borrowed_total = borrowed_total + candidate['borrowed_count']
            if next_borrowed_total > MAX_TOTAL_BORROWED:
                continue
            if candidate['owned_mask'] & used_owned_mask:
                continue

            chosen_pairs[task_index] = (task_entry, candidate)
            dfs(
                task_index + 1,
                used_owned_mask | candidate['owned_mask'],
                next_borrowed_total,
                add_tier_counts(tier_counts, candidate['tier_counts']),
                total_pets + candidate['pet_count'],
                total_score + candidate['score']
            )

    dfs(0, 0, 0, (0, 0, 0, 0, 0), 0, 0)
    return best_solution


def summarize_solution(solution: Dict) -> Tuple[Dict, List[List[Dict]], bool]:
    if solution is None:
        best_score = {
            'total': -1,
            'borrowed': float('inf'),
            'total_pets': float('inf'),
            'tier_counts': (0, 0, 0, 0, 0),
            'objective': None
        }
        return best_score, [], False

    best_score = {
        'total': solution['total'],
        'borrowed': solution['borrowed'],
        'total_pets': solution['total_pets'],
        'tier_counts': solution['tier_counts'],
        'objective': solution['objective']
    }
    return best_score, [solution['assignments']], solution['all_special']


def generate_task_combinations(tasks: List[Dict], task_count: int) -> List[Tuple[Dict]]:
    """生成指定数量的任务组合，优先选择加成技能多的任务"""
    # 过滤掉空任务
    valid_tasks = [task for task in tasks if task['task']]
    # 按加成技能数量排序，优先计算加成技能多的任务组合
    valid_tasks.sort(key=lambda x: len(x['bonus_skills']), reverse=True)
    # 生成指定数量的任务组合
    if len(valid_tasks) >= task_count:
        return list(itertools.combinations(valid_tasks, task_count))
    else:
        # 如果有效任务数量不足，返回空列表
        return []


def get_combination_upper_bound(task_entries: List[Dict]) -> Tuple[bool, Tuple[int, int, int, int, int, int, int, int]]:
    tier_counts = (0, 0, 0, 0, 0)
    min_borrowed = 0
    min_pets = 0
    max_score = 0
    all_special_possible = True

    for task_entry in task_entries:
        tier_counts = add_tier_counts(tier_counts, task_entry['best_counts'])
        min_borrowed += task_entry['min_borrowed']
        min_pets += task_entry['min_pet_count']
        max_score += task_entry['max_score']
        if task_entry['best_tier_rank'] != SPECIAL_TIER_RANK:
            all_special_possible = False

    return all_special_possible, build_objective(tier_counts, min_borrowed, min_pets, max_score)


def calculate_best_assignment(task_combination: Tuple[Dict], available_pets: List[Dict], pet_task_scores: Dict[int, Dict[int, int]]) -> Tuple[Dict, List[List[Dict]], bool]:
    """计算给定任务组合的最佳宠物分配"""
    task_entries = build_task_entries(task_combination, available_pets, pet_task_scores)
    solution = search_all_special(task_entries)
    if solution is not None:
        return summarize_solution(solution)

    solution = search_best_assignment(task_entries)
    return summarize_solution(solution)
class DispatchCalculatorGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("宠物派遣计算器")
        # 适配10列的宽度，纵向自动拉满屏幕工作区高度
        self.root.update()  # 先更新窗口，获取正确的坐标信息
        # 获取标题栏高度
        title_bar_height = self.root.winfo_rooty() - self.root.winfo_y()
        # 获取工作区高度（去掉任务栏的屏幕高度）
        import sys
        if sys.platform == "win32":
            # Windows系统调用API获取工作区
            from ctypes import windll, byref, Structure, c_int
            class RECT(Structure):
                _fields_ = [("left", c_int), ("top", c_int), ("right", c_int), ("bottom", c_int)]
            work_rect = RECT()
            windll.user32.SystemParametersInfoW(0x30, 0, byref(work_rect), 0)
            work_height = work_rect.bottom - work_rect.top
        else:
            # 其他系统，默认减去任务栏高度
            work_height = self.root.winfo_screenheight() - 40
        # 计算客户区可用高度
        client_height = work_height - title_bar_height
        # 设置窗口大小：宽度保持1200，高度为计算出的客户区高度，窗口顶部对齐屏幕顶部
        self.root.geometry(f"1200x{client_height}+0+0")
        
        # 初始化变量
        self.pets = []
        self.regions = {}
        self.selected_region = None
        self.owned_pets = []
        self.farm_pets = []
        self.task_count = 1
        self.persist_enabled = False
        
        # 图片选择相关的变量
        self.owned_selected = set()
        self.farm_selected = set()
        self.pet_images = {}  # 保存图片引用，防止垃圾回收
        self.owned_buttons = {}
        self.farm_buttons = {}
        
        # 创建界面元素
        self.create_widgets()
        self.root.after_idle(self.configure_window_constraints)
        
        # 加载数据
        self.load_data()
    
    def toggle_owned_pet(self, pet_id):
        """切换自有宠物的选中状态"""
        if pet_id in self.owned_selected:
            # 取消选中
            self.owned_selected.remove(pet_id)
        else:
            # 选中
            self.owned_selected.add(pet_id)
        self.update_pet_button_state(self.owned_buttons, pet_id, pet_id in self.owned_selected)
        self.persist_selection_state()
    
    def toggle_farm_pet(self, pet_id):
        """切换农场宠物的选中状态"""
        if pet_id in self.farm_selected:
            # 取消选中
            self.farm_selected.remove(pet_id)
        else:
            # 选中
            self.farm_selected.add(pet_id)
        self.update_pet_button_state(self.farm_buttons, pet_id, pet_id in self.farm_selected)
        self.persist_selection_state()
    
    def load_data(self):
        # 检查依赖
        if not check_dependencies():
            messagebox.showerror("错误", "需要安装pandas、openpyxl和pillow库，请运行以下命令：\npip install pandas openpyxl pillow")
            self.root.quit()
            return
        
        # 导入PIL模块
        from PIL import Image, ImageTk
        
        # 读取宠物数据
        try:
            self.pets = read_pet_list()
        except FileNotFoundError as e:
            messagebox.showerror("错误", str(e))
            self.root.quit()
            return
        
        # 读取地区数据
        try:
            self.regions = read_regions()
        except FileNotFoundError as e:
            messagebox.showerror("错误", str(e))
            self.root.quit()
            return
        
        # 填充区域下拉框
        self.region_combobox["values"] = list(self.regions.keys())
        if self.regions:
            self.region_combobox.current(0)
            self.on_region_selected(None)
        
        # 填充宠物图片按钮
        score_to_level = {v:k for k,v in skill_score_map.items()}
        # 初始化自有宠物的网格列权重，10列均分
        for col in range(COLS_PER_ROW):
            self.owned_scroll_frame.grid_columnconfigure(col, weight=1)
        
        # 初始化农场宠物的网格列权重，10列均分，和自有宠物完全一致
        for col in range(COLS_PER_ROW):
            self.farm_scroll_frame.grid_columnconfigure(col, weight=1)
        
        for idx, pet in enumerate(self.pets):
            # 加载宠物图片，从images目录加载，自动缩放到统一大小
            try:
                img_path = f"./images/{pet['name']}.png"
                # 用PIL打开图片
                pil_img = Image.open(img_path)
                # 缩放到统一的64x64大小，保证所有图片大小一致，完整显示
                pil_img = pil_img.resize((PET_ICON_SIZE, PET_ICON_SIZE), Image.Resampling.LANCZOS)
                # 转成tkinter可用的格式
                img = ImageTk.PhotoImage(pil_img)
                self.pet_images[pet['id']] = img
            except Exception as e:
                # 图片加载失败时自动降级为文字按钮
                img = None
                print(f"警告：加载宠物{pet['name']}的图片失败，将使用文字按钮: {e}")
            
            # 按钮上的文字信息，保留原有的宠物详情
            skill_str = ', '.join([f"{k}({score_to_level[v]})" for k, v in pet['skills'].items()])
            btn_text = f"{pet['name']}\n{pet['rarity']}\n{skill_str}"
            
            # 计算网格位置，固定每行10个
            row = idx // COLS_PER_ROW
            col = idx % COLS_PER_ROW
            
            # 创建自有宠物的按钮，网格布局，10列均分
            if img:
                owned_btn = tk.Button(
                    self.owned_scroll_frame,
                    image=img,
                    text=btn_text,
                    compound=tk.TOP,  # 图片在上，文字在下
                    command=lambda pid=pet['id']: self.toggle_owned_pet(pid),
                    wraplength=90  # 文字自动换行，适配按钮宽度
                )
            else:
                owned_btn = tk.Button(
                    self.owned_scroll_frame,
                    text=btn_text,
                    command=lambda pid=pet['id']: self.toggle_owned_pet(pid),
                    wraplength=90
                )
            # 网格布局，sticky=NSEW让按钮填满整个单元格，保证所有按钮大小相同
            owned_btn.grid(row=row, column=col, padx=3, pady=3, sticky=tk.NSEW)
            self.owned_buttons[pet['id']] = owned_btn
            
            # 创建农场宠物的按钮，和自有宠物完全相同的布局
            if img:
                farm_btn = tk.Button(
                    self.farm_scroll_frame,
                    image=img,
                    text=btn_text,
                    compound=tk.TOP,
                    command=lambda pid=pet['id']: self.toggle_farm_pet(pid),
                    wraplength=90
                )
            else:
                farm_btn = tk.Button(
                    self.farm_scroll_frame,
                    text=btn_text,
                    command=lambda pid=pet['id']: self.toggle_farm_pet(pid),
                    wraplength=90
                )
            # 网格布局，sticky=NSEW让按钮填满整个单元格，保证所有按钮大小相同
            farm_btn.grid(row=row, column=col, padx=3, pady=3, sticky=tk.NSEW)
            self.farm_buttons[pet['id']] = farm_btn

        self.restore_selection_state()
        self.persist_enabled = True
        self.persist_selection_state()

    def update_pet_button_state(self, button_map: Dict[int, tk.Button], pet_id: int, is_selected: bool):
        btn = button_map.get(pet_id)
        if btn is None:
            return
        if is_selected:
            btn.config(relief=tk.SUNKEN, bg="lightblue")
        else:
            btn.config(relief=tk.RAISED, bg="SystemButtonFace")

    def refresh_pet_selection_states(self):
        for pet_id in self.owned_buttons:
            self.update_pet_button_state(self.owned_buttons, pet_id, pet_id in self.owned_selected)
        for pet_id in self.farm_buttons:
            self.update_pet_button_state(self.farm_buttons, pet_id, pet_id in self.farm_selected)

    def get_selection_state_payload(self) -> Dict:
        return {
            "selected_region": self.selected_region,
            "task_count": self.task_count_var.get(),
            "owned_selected": sorted(self.owned_selected),
            "farm_selected": sorted(self.farm_selected)
        }

    def persist_selection_state(self):
        if not self.persist_enabled:
            return
        try:
            save_selection_state(self.get_selection_state_payload())
        except OSError as e:
            print(f"警告：保存选择配置失败：{e}")

    def restore_selection_state(self):
        state = load_selection_state()
        if not state:
            return

        saved_region = state.get("selected_region")
        if saved_region in self.regions:
            self.region_var.set(saved_region)
            self.selected_region = saved_region
        elif self.regions and not self.selected_region:
            self.region_combobox.current(0)
            self.on_region_selected(None)

        saved_task_count = str(state.get("task_count", "")).strip()
        valid_task_counts = set(self.task_count_combobox["values"])
        if saved_task_count in valid_task_counts:
            self.task_count_var.set(saved_task_count)

        valid_pet_ids = {pet["id"] for pet in self.pets}
        self.owned_selected = {
            pet_id for pet_id in state.get("owned_selected", [])
            if isinstance(pet_id, int) and pet_id in valid_pet_ids
        }
        self.farm_selected = {
            pet_id for pet_id in state.get("farm_selected", [])
            if isinstance(pet_id, int) and pet_id in valid_pet_ids
        }
        self.refresh_pet_selection_states()
    
    def on_region_selected(self, event):
        self.selected_region = self.region_var.get()
        self.persist_selection_state()

    def on_task_count_selected(self, event):
        self.persist_selection_state()

    def configure_window_constraints(self):
        self.root.update_idletasks()
        min_height = (
            self.title_label.winfo_reqheight()
            + self.region_frame.winfo_reqheight()
            + self.bottom_frame.winfo_reqheight()
            + MIN_PET_LIST_HEIGHT * 2
            + 50
        )
        self.root.minsize(960, min_height)

    def get_scrollable_canvas(self, widget):
        while widget is not None:
            if widget in (self.owned_canvas, self.farm_canvas):
                return widget
            widget = widget.master
        return None

    def on_pet_list_mousewheel(self, event):
        widget = self.root.winfo_containing(event.x_root, event.y_root)
        canvas = self.get_scrollable_canvas(widget)
        if canvas is None:
            return

        delta = getattr(event, "delta", 0)
        if delta:
            steps = -int(delta / 120)
        elif getattr(event, "num", None) == 4:
            steps = -1
        elif getattr(event, "num", None) == 5:
            steps = 1
        else:
            return

        if steps != 0:
            canvas.yview_scroll(steps, "units")
            return "break"
    
    def create_widgets(self):
        self.root.grid_columnconfigure(0, weight=1)
        self.root.grid_rowconfigure(2, weight=1)

        # 标题标签
        self.title_label = ttk.Label(self.root, text="宠物派遣计算器", font=("Arial", 16), anchor=tk.CENTER, justify=tk.CENTER)
        self.title_label.grid(row=0, column=0, padx=10, pady=(10, 5), sticky=tk.EW)
        
        # 区域选择框架
        self.region_frame = ttk.LabelFrame(self.root, text="选择派遣区域")
        self.region_frame.grid(row=1, column=0, padx=10, pady=5, sticky=tk.EW)
        
        self.region_var = tk.StringVar()
        self.region_combobox = ttk.Combobox(self.region_frame, textvariable=self.region_var, state="readonly")
        self.region_combobox.pack(padx=10, pady=5, fill=tk.X)
        self.region_combobox.bind("<<ComboboxSelected>>", self.on_region_selected)

        self.pet_container = ttk.Frame(self.root)
        self.pet_container.grid(row=2, column=0, padx=10, pady=5, sticky=tk.NSEW)
        self.pet_container.grid_columnconfigure(0, weight=1)
        self.pet_container.grid_rowconfigure(0, weight=1, uniform="pet_lists")
        self.pet_container.grid_rowconfigure(1, weight=1, uniform="pet_lists")
        
        # 宠物选择框架
        pet_frame = ttk.LabelFrame(self.pet_container, text="选择拥有的宠物（点击图片选择/取消选择）")
        pet_frame.grid(row=0, column=0, pady=(0, 5), sticky=tk.NSEW)
        pet_frame.grid_columnconfigure(0, weight=1)
        pet_frame.grid_rowconfigure(0, weight=1)
        
        # 自有宠物的滚动容器
        self.owned_canvas = tk.Canvas(pet_frame, highlightthickness=0)
        owned_scroll = ttk.Scrollbar(pet_frame, orient=tk.VERTICAL, command=self.owned_canvas.yview)
        self.owned_scroll_frame = ttk.Frame(self.owned_canvas)
        self.owned_scroll_frame.bind(
            "<Configure>",
            lambda e: self.owned_canvas.configure(
                scrollregion=self.owned_canvas.bbox("all")
            )
        )
        self.owned_canvas_window = self.owned_canvas.create_window(
            (0, 0),
            window=self.owned_scroll_frame,
            anchor="nw",
            width=self.owned_canvas.winfo_width()
        )
        self.owned_canvas.configure(yscrollcommand=owned_scroll.set)

        def on_owned_canvas_configure(event):
            self.owned_canvas.itemconfig(self.owned_canvas_window, width=event.width)

        self.owned_canvas.bind("<Configure>", on_owned_canvas_configure)
        self.owned_canvas.grid(row=0, column=0, sticky=tk.NSEW)
        owned_scroll.grid(row=0, column=1, sticky=tk.NS)
        
        # 农场宠物选择框架
        farm_frame = ttk.LabelFrame(self.pet_container, text="选择农场宠物（点击图片选择/取消选择）")
        farm_frame.grid(row=1, column=0, sticky=tk.NSEW)
        farm_frame.grid_columnconfigure(0, weight=1)
        farm_frame.grid_rowconfigure(0, weight=1)
        
        # 农场宠物的滚动容器
        self.farm_canvas = tk.Canvas(farm_frame, highlightthickness=0)
        farm_scroll = ttk.Scrollbar(farm_frame, orient=tk.VERTICAL, command=self.farm_canvas.yview)
        self.farm_scroll_frame = ttk.Frame(self.farm_canvas)
        self.farm_scroll_frame.bind(
            "<Configure>",
            lambda e: self.farm_canvas.configure(
                scrollregion=self.farm_canvas.bbox("all")
            )
        )
        self.farm_canvas_window = self.farm_canvas.create_window(
            (0, 0),
            window=self.farm_scroll_frame,
            anchor="nw",
            width=self.farm_canvas.winfo_width()
        )
        self.farm_canvas.configure(yscrollcommand=farm_scroll.set)

        def on_farm_canvas_configure(event):
            self.farm_canvas.itemconfig(self.farm_canvas_window, width=event.width)

        self.farm_canvas.bind("<Configure>", on_farm_canvas_configure)
        self.farm_canvas.grid(row=0, column=0, sticky=tk.NSEW)
        farm_scroll.grid(row=0, column=1, sticky=tk.NS)
        self.root.bind_all("<MouseWheel>", self.on_pet_list_mousewheel, add="+")
        self.root.bind_all("<Button-4>", self.on_pet_list_mousewheel, add="+")
        self.root.bind_all("<Button-5>", self.on_pet_list_mousewheel, add="+")
        
        # 底部固定区域，保证任务选择和计算按钮始终可见
        self.bottom_frame = ttk.Frame(self.root)
        self.bottom_frame.grid(row=3, column=0, sticky=tk.EW)
        self.bottom_frame.grid_columnconfigure(0, weight=1)
        
        # 任务数量选择
        task_frame = ttk.LabelFrame(self.bottom_frame, text="选择任务数量")
        task_frame.grid(row=0, column=0, padx=10, pady=(5, 0), sticky=tk.EW)
        
        self.task_count_var = tk.StringVar()
        self.task_count_combobox = ttk.Combobox(task_frame, textvariable=self.task_count_var, values=["1", "2", "3", "4", "5"], state="readonly")
        self.task_count_combobox.current(0)
        self.task_count_combobox.pack(padx=10, pady=5, fill=tk.X)
        self.task_count_combobox.bind("<<ComboboxSelected>>", self.on_task_count_selected)
        
        # 计算按钮
        calc_button = ttk.Button(self.bottom_frame, text="计算最优派遣方案", command=self.calculate)
        calc_button.grid(row=1, column=0, padx=10, pady=10)
    
    def calculate(self):
        # 获取选择的区域
        if not self.selected_region:
            messagebox.showwarning("警告", "请先选择派遣区域")
            return
        
        # 获取选择的拥有的宠物
        if not self.owned_selected:
            messagebox.showwarning("警告", "请选择至少一只拥有的宠物")
            return
        self.owned_pets = [pet for pet in self.pets if pet['id'] in self.owned_selected]
        
        # 获取选择的农场宠物
        self.farm_pets = []
        for pet_id in self.farm_selected:
            pet = next(p for p in self.pets if p['id'] == pet_id).copy()
            pet['is_borrowed'] = True
            self.farm_pets.append(pet)
        
        # 获取任务数量
        try:
            self.task_count = int(self.task_count_var.get())
        except ValueError:
            messagebox.showwarning("警告", "请选择有效的任务数量")
            return
        self.persist_selection_state()
        
        # 参数检查通过，创建结果弹出窗口
        result_window = tk.Toplevel(self.root)
        result_window.title("最优派遣方案结果")
        result_window.geometry("800x600")
        
        # 在新窗口中创建结果显示区域
        result_frame = ttk.LabelFrame(result_window, text="计算结果")
        result_frame.pack(padx=10, pady=5, fill=tk.BOTH, expand=True)
        
        result_text = scrolledtext.ScrolledText(result_frame, wrap=tk.WORD)
        result_text.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        result_text.config(state=tk.NORMAL)
        
        # 开始计算
        result_text.delete(1.0, tk.END)
        result_text.insert(tk.END, "正在计算，请稍候...\n")
        result_text.update()
        
        # 预计算宠物-任务得分矩阵
        available_pets = self.owned_pets + self.farm_pets
        tasks = self.regions[self.selected_region]
        pet_task_scores = precompute_pet_task_scores(available_pets, tasks)
        
        # 生成任务组合
        task_combinations = generate_task_combinations(tasks, self.task_count)
        if not task_combinations:
            result_text.insert(tk.END, f"无法生成{self.task_count}个任务组合，该区域只有{len([t for t in tasks if t['task']])}个有效任务，请选择较小的任务数量。\n")
            result_text.config(state=tk.DISABLED)
            return
        
        # 初始化全局最佳
        overall_best = {
            'total': -1,
            'borrowed': float('inf'),
            'total_pets': float('inf'),
            'tier_counts': (0, 0, 0, 0, 0),
            'objective': None,
            'assignments': []
        }
        
        # 记录开始时间
        start_time = time.time()
        
        # 使用优化后的顺序搜索，优先计算更有希望全特阶的任务组合
        result_text.insert(tk.END, "正在使用优化后的搜索器计算最优派遣方案...\n")
        result_text.update()
        
        try:
            prepared_combinations = []
            for task_combo in task_combinations:
                valid_tasks = tuple(task for task in task_combo if task['task'])
                if not valid_tasks:
                    continue
                task_entries = build_task_entries(valid_tasks, available_pets, pet_task_scores)
                all_special_possible, upper_bound = get_combination_upper_bound(task_entries)
                prepared_combinations.append({
                    'tasks': valid_tasks,
                    'task_entries': task_entries,
                    'all_special_possible': all_special_possible,
                    'upper_bound': upper_bound
                })

            prepared_combinations.sort(
                key=lambda item: (item['all_special_possible'], item['upper_bound']),
                reverse=True
            )

            result_text.insert(tk.END, f"正在计算任务组合，共 {len(prepared_combinations)} 组...\n")
            result_text.update()

            for prepared in prepared_combinations:
                if (
                    overall_best['objective'] is not None
                    and prepared['upper_bound'] <= overall_best['objective']
                ):
                    continue

                best_score, best_assignments, combo_all_special = summarize_solution(
                    search_all_special(prepared['task_entries'])
                )
                if not combo_all_special:
                    best_score, best_assignments, combo_all_special = summarize_solution(
                        search_best_assignment(prepared['task_entries'])
                    )

                if not best_assignments:
                    continue

                if combo_all_special:
                    overall_best['total'] = best_score['total']
                    overall_best['borrowed'] = best_score['borrowed']
                    overall_best['total_pets'] = best_score['total_pets']
                    overall_best['tier_counts'] = best_score['tier_counts']
                    overall_best['objective'] = best_score['objective']
                    overall_best['assignments'] = best_assignments
                    break

                if (
                    overall_best['objective'] is None
                    or best_score['objective'] > overall_best['objective']
                ):
                    overall_best['total'] = best_score['total']
                    overall_best['borrowed'] = best_score['borrowed']
                    overall_best['total_pets'] = best_score['total_pets']
                    overall_best['tier_counts'] = best_score['tier_counts']
                    overall_best['objective'] = best_score['objective']
                    overall_best['assignments'] = best_assignments
            
            # 计算总耗时
            end_time = time.time()
            total_calc_time = end_time - start_time
            
            # 输出结果
            result_text.insert(tk.END, "\n===== 最优派遣方案结果 =====\n")
            result_text.insert(tk.END, f"✅ 计算完成！方案计算总耗时：{total_calc_time:.2f} 秒\n")
            result_text.insert(tk.END, f"派遣区域：{self.selected_region}\n")
            
            if not overall_best['assignments']:
                result_text.insert(tk.END, "没有找到有效的派遣方案。\n")
            else:
                # 取第一个最佳方案
                best_assignment = overall_best['assignments'][0]
                result_text.insert(tk.END, f"执行任务数量：{len(best_assignment)}\n")
                result_text.insert(tk.END, f"总得分：{overall_best['total']}\n")
                result_text.insert(tk.END, f"借用宠物数量：{overall_best['borrowed']}\n")
                result_text.insert(tk.END, f"总使用宠物数量：{overall_best['total_pets']}\n")
                
                # 输出每个任务
                for i, assign in enumerate(best_assignment, 1):
                    task = assign['task']
                    team = assign['team']
                    score = assign['score']
                    reward_level = get_reward_level(score)
                    
                    result_text.insert(tk.END, f"\n--- 任务{i} ---\n")
                    result_text.insert(tk.END, f"任务名称：{task['task']}\n")
                    result_text.insert(tk.END, f"任务区域：{task['area']}\n")
                    result_text.insert(tk.END, f"加成特性：{', '.join(task['bonus_skills']) if task['bonus_skills'] else '无'}\n")
                    
                    # 处理宠物名称，农场宠物加（借）
                    pet_names = []
                    for pet in team:
                        if pet.get('is_borrowed', False):
                            pet_names.append(f"{pet['name']}（借）")
                        else:
                            pet_names.append(pet['name'])
                    result_text.insert(tk.END, f"推荐派遣宠物：{', '.join(pet_names)}\n")
                    result_text.insert(tk.END, f"任务得分：{score}\n")
                    result_text.insert(tk.END, f"预计奖励等级：{reward_level}\n")
                
                # 如果有多个同优先方案，提示用户
                if len(overall_best['assignments']) > 1:
                    result_text.insert(tk.END, f"\n注：共有{len(overall_best['assignments'])}种同优先的最优方案，以上为其中一种。\n")
        
        except Exception as e:
            result_text.insert(tk.END, f"\n计算过程中出错：{e}\n")
        
        result_text.config(state=tk.DISABLED)
if __name__ == "__main__":
    # 处理打包后的多进程支持
    multiprocessing.freeze_support()
    # 设置启动方式为spawn，兼容Windows打包
    try:
        multiprocessing.set_start_method('spawn', force=True)
    except RuntimeError:
        # 如果已经设置过启动方式，忽略错误
        pass
    
    root = tk.Tk()
    app = DispatchCalculatorGUI(root)
    root.mainloop()
