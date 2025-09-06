# 🚀 Git Cheatsheet – Projet eTix

## 🔎 Vérifier l’état du repo
```bash
git status
git add .
git add app/src/main/java/com/etix/.../TicketDao.kt
git commit -m "feat: courte description de ta modif"
Conventions de message
	•	feat: nouvelle fonctionnalité
	•	fix: correction de bug
	•	chore: maintenance / config
	•	refactor: réorganisation du code
	•	docs: documentation
	•	test: ajout/modif de tests

🌱 Branches
git branch -vv
git checkout -b feature/ma-feature
git checkout dev
Pousser vers GitHub
	•	Branche déjà suivie :
git push
	•	Première fois :
git push -u origin ma-branche
Pull Request (PR)
1.	Pousser la branche
	2.	GitHub → Pull requests → New pull request
	3.	base = dev, compare = ta branche
	4.	Créer PR → QA → Merge
🔄 Sync du matin
git checkout dev
git pull
📅 Workflow quotidien
	•	Matin : git sync
	•	Journée : git add . && git commit -m "feat: ..."
	•	Soir : git save
⚡️ Alias utiles
git config --global alias.save '!f(){ git add . && git commit -m "chore: sauvegarde fin de journée $(date "+%Y-%m-%d %H:%M")" && git push; }; f'
git config --global alias.sync '!f(){ git checkout dev && git pull; }; f'
git config --global push.autoSetupRemote true

